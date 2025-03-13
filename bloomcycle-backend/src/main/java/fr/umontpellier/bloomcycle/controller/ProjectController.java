package fr.umontpellier.bloomcycle.controller;

import fr.umontpellier.bloomcycle.dto.ProjectResponse;
import fr.umontpellier.bloomcycle.dto.container.ContainerResponse;
import fr.umontpellier.bloomcycle.dto.error.ErrorResponse;
import fr.umontpellier.bloomcycle.dto.ProjectDetailResponse;
import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.model.User;
import fr.umontpellier.bloomcycle.model.container.ContainerStatus;
import fr.umontpellier.bloomcycle.model.container.ContainerOperation;
import fr.umontpellier.bloomcycle.service.DockerService;
import fr.umontpellier.bloomcycle.service.ProjectService;
import fr.umontpellier.bloomcycle.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Project management endpoints")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;
    private final DockerService dockerService;
    private final FileService fileService;

    @Operation(
        summary = "Create project from Git",
        description = "Initialize a new project from a Git repository"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Project created successfully",
        content = @Content(schema = @Schema(implementation = ProjectResponse.class))
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - JWT token is missing or invalid"
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error while creating project"
    )
    @SecurityRequirement(name = "bearer-key")
    @PostMapping("/git")
    public ResponseEntity<Project> createProjectFromGit(
            @Parameter(description = "Name of the project") @RequestParam String projectName,
            @Parameter(description = "Git repository URL") @RequestParam String gitUrl) {
        try {
            var project = projectService.initializeProjectFromGit(projectName, gitUrl);
            return ResponseEntity.ok(project);
        } catch (Exception e) {
            log.error("Error creating project from git: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Create project from ZIP",
        description = "Initialize a new project from a ZIP file containing the source code"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Project created successfully",
        content = @Content(schema = @Schema(implementation = ProjectResponse.class))
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - JWT token is missing or invalid"
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error while creating project"
    )
    @SecurityRequirement(name = "bearer-key")
    @PostMapping("/zip")
    public ResponseEntity<Project> createProjectFromZip(
            @Parameter(description = "Name of the project") @RequestParam String projectName,
            @Parameter(description = "ZIP file containing project source code") @RequestParam("file") MultipartFile sourceZip) {
        try {
            var project = projectService.initializeProjectFromZip(projectName, sourceZip);
            return ResponseEntity.ok(project);
        } catch (Exception e) {
            log.error("Error creating project from zip: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void checkProjectOwnership(Project project) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var currentUser = (User) authentication.getPrincipal();

        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to access this project");
        }
    }

    @Operation(
        summary = "Create a new project",
        description = "Create a project from either a Git repository or a ZIP file"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Project created successfully",
        content = @Content(schema = @Schema(implementation = ProjectResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid input",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - JWT token is missing or invalid"
    )
    @SecurityRequirement(name = "bearer-key")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> createProject(
            @Parameter(description = "Project name") @RequestParam("name") String name,
            @Parameter(description = "Git repository URL") @RequestParam(value = "gitUrl", required = false) String gitUrl,
            @Parameter(description = "Source code as ZIP file") @RequestParam(value = "sourceZip", required = false) MultipartFile sourceZip) {
        try {
            Project project;
            if (gitUrl != null && !gitUrl.isEmpty()) {
                project = projectService.initializeProjectFromGit(name, gitUrl);
            } else if (sourceZip != null && !sourceZip.isEmpty()) {
                project = projectService.initializeProjectFromZip(name, sourceZip);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "You must provide either gitUrl or a ZIP file"
                ));
            }
            return ResponseEntity.ok(ProjectResponse.fromProject(project, ContainerStatus.STOPPED));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "details", e.getCause() != null ? e.getCause().getMessage() : "No additional details"
            ));
        }
    }

    @Operation(
        summary = "Get current user's projects",
        description = "Retrieves all projects belonging to the authenticated user with their current container status"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Projects successfully retrieved",
        content = @Content(
            mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = ProjectResponse.class))
        )
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - JWT token is missing or invalid",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Bad request - Error while retrieving projects",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class)
        )
    )
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/me")
    public ResponseEntity<List<ProjectResponse>> getCurrentUserProjects() {
        try {
            var projects = projectService.getCurrentUserProjects()
                    .stream()
                    .map(project -> {
                        var containerStatus = dockerService.getProjectStatus(project.getId());
                        return ProjectResponse.fromProject(project, containerStatus);
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            log.error("Error getting current user's projects", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    @Operation(
        summary = "Get project by ID",
        description = "Retrieve a specific project by its ID if the authenticated user owns it"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Project found and returned",
        content = @Content(schema = @Schema(implementation = ProjectResponse.class))
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - JWT token is missing or invalid"
    )
    @ApiResponse(
        responseCode = "403",
        description = "Forbidden - User doesn't own this project"
    )
    @ApiResponse(
        responseCode = "404",
        description = "Project not found"
    )
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable String id) {
        try {
            var project = projectService.getProjectById(id);
            checkProjectOwnership(project);

            var containerStatus = dockerService.getProjectStatus(id);
            return ResponseEntity.ok(ProjectResponse.fromProject(project, containerStatus));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @Operation(summary = "Get all projects")
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        try {
            var projects = projectService.getAllProjects()
                    .stream()
                    .map(project -> {
                        var containerStatus = dockerService.getProjectStatus(project.getId());
                        return ProjectResponse.fromProject(project, containerStatus);
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            log.error("Error getting all projects", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    @Operation(
        summary = "Delete a project",
        description = "Delete a project and all its associated resources. Only the project owner can delete it."
    )
    @ApiResponse(
        responseCode = "204",
        description = "Project successfully deleted"
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - JWT token is missing or invalid"
    )
    @ApiResponse(
        responseCode = "403",
        description = "Forbidden - User doesn't own this project"
    )
    @ApiResponse(
        responseCode = "404",
        description = "Project not found"
    )
    @SecurityRequirement(name = "bearer-key")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "ID of the project to delete")
            @PathVariable String id) {
        try {
            var project = projectService.getProjectById(id);
            checkProjectOwnership(project);
            projectService.deleteProject(id);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error deleting project {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Start a project's containers",
        description = "Start all containers associated with the specified project"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Project containers started successfully",
        content = @Content(schema = @Schema(implementation = ContainerResponse.class))
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - JWT token is missing or invalid"
    )
    @ApiResponse(
        responseCode = "403",
        description = "Forbidden - User doesn't own this project"
    )
    @ApiResponse(
        responseCode = "500",
        description = "Error starting project containers"
    )
    @SecurityRequirement(name = "bearer-key")
    @PostMapping("/{id}/start")
    public ResponseEntity<ContainerResponse> startProject(@PathVariable String id) {
        try {
            var project = projectService.getProjectById(id);
            checkProjectOwnership(project);

            var containerInfo = dockerService.executeOperation(id, ContainerOperation.START)
                    .get(30, TimeUnit.SECONDS);  // Timeout après 30 secondes
            return ResponseEntity.ok(ContainerResponse.fromContainerInfo(containerInfo, "start"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ContainerResponse.error("start", e.getMessage()));
        }
    }

    @Operation(
        summary = "Stop a project's containers",
        description = "Stop all containers associated with the specified project"
    )
    @ApiResponse(
        responseCode = "202",
        description = "Stop operation accepted",
        content = @Content(schema = @Schema(implementation = ContainerResponse.class))
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - JWT token is missing or invalid"
    )
    @ApiResponse(
        responseCode = "403",
        description = "Forbidden - User doesn't own this project"
    )
    @ApiResponse(
        responseCode = "500",
        description = "Error stopping project containers"
    )
    @SecurityRequirement(name = "bearer-key")
    @PostMapping("/{id}/stop")
    public ResponseEntity<ContainerResponse> stopProject(@PathVariable String id) {
        try {
            var project = projectService.getProjectById(id);
            checkProjectOwnership(project);

            var containerInfo = dockerService.executeOperation(id, ContainerOperation.STOP)
                    .get(30, TimeUnit.SECONDS);
            return ResponseEntity.accepted()
                    .body(ContainerResponse.fromContainerInfo(containerInfo, "stop"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ContainerResponse.error("stop", e.getMessage()));
        }
    }

    @Operation(
        summary = "Restart a project's containers",
        description = "Restart all containers associated with the specified project"
    )
    @ApiResponse(
        responseCode = "202",
        description = "Restart operation accepted",
        content = @Content(schema = @Schema(implementation = ContainerResponse.class))
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - JWT token is missing or invalid"
    )
    @ApiResponse(
        responseCode = "403",
        description = "Forbidden - User doesn't own this project"
    )
    @ApiResponse(
        responseCode = "500",
        description = "Error restarting project containers"
    )
    @SecurityRequirement(name = "bearer-key")
    @PostMapping("/{id}/restart")
    public ResponseEntity<ContainerResponse> restartProject(@PathVariable String id) {
        try {
            var project = projectService.getProjectById(id);
            checkProjectOwnership(project);

            var containerInfo = dockerService.executeOperation(id, ContainerOperation.RESTART)
                    .get(30, TimeUnit.SECONDS);
            return ResponseEntity.accepted()
                    .body(ContainerResponse.fromContainerInfo(containerInfo, "restart"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ContainerResponse.error("restart", e.getMessage()));
        }
    }

    @Operation(
        summary = "Get project containers status",
        description = "Get the current status of all containers for a specific project"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Project status retrieved successfully",
        content = @Content(schema = @Schema(implementation = ContainerResponse.class))
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - JWT token is missing or invalid"
    )
    @ApiResponse(
        responseCode = "403",
        description = "Forbidden - User doesn't own this project"
    )
    @ApiResponse(
        responseCode = "404",
        description = "Project not found"
    )
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/{id}/status")
    public ResponseEntity<ContainerResponse> getProjectStatus(@PathVariable String id) {
        try {
            var project = projectService.getProjectById(id);
            checkProjectOwnership(project);

            var status = dockerService.getProjectStatus(id);
            return ResponseEntity.ok(ContainerResponse.fromStatus(status));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ContainerResponse.error("get status", e.getMessage()));
        }
    }

    @Operation(
        summary = "Get project details",
        description = "Get detailed information about a specific project"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Project details retrieved successfully",
        content = @Content(schema = @Schema(implementation = ProjectDetailResponse.class))
    )
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/{id}/details")
    public ResponseEntity<ProjectDetailResponse> getProjectDetails(@PathVariable String id) {
        try {
            var project = projectService.getProjectById(id);
            checkProjectOwnership(project);

            var status = dockerService.getProjectStatus(id);
            var cpuUsage = dockerService.getContainerMetrics(project)[0];
            var memoryUsage = dockerService.getContainerMetrics(project)[1];
            var technology = projectService.getProjectTechnology(id);
            var serverUrl = status == ContainerStatus.RUNNING ? 
                dockerService.getProjectUrl(id) : null;

            return ResponseEntity.ok(ProjectDetailResponse.fromProject(
                project, status, cpuUsage, memoryUsage, serverUrl, technology));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}