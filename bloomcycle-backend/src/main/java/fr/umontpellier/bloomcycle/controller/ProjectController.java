package fr.umontpellier.bloomcycle.controller;

import fr.umontpellier.bloomcycle.dto.AutoRestartRequest;
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

    private void checkProjectOwnership(Project project) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var currentUser = (User) authentication.getPrincipal();

        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to access this project");
        }
    }

    @Operation(
        summary = "Create a new project",
        description = "Create a project from either a Git repository URL or a ZIP file containing the source code"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Project created successfully",
        content = @Content(schema = @Schema(implementation = ProjectResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid input - Must provide either gitUrl or a ZIP file, but not both",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> createProject(
            @Parameter(description = "Project name", required = true) @RequestParam("name") String name,
            @Parameter(description = "Git repository URL") @RequestParam(value = "gitUrl", required = false) String gitUrl,
            @Parameter(description = "Source code as ZIP file") @RequestParam(value = "sourceZip", required = false) MultipartFile sourceZip) {
        try {
            boolean hasGitUrl = gitUrl != null && !gitUrl.trim().isEmpty();
            boolean hasZipFile = sourceZip != null && !sourceZip.isEmpty();
            
            ResponseEntity<Object> validationResponse = null;
            if (hasGitUrl && hasZipFile) {
                validationResponse = ResponseEntity.badRequest().body(Map.of(
                    "error", "You must provide either gitUrl or a ZIP file, but not both"
                ));
            } else if (!hasGitUrl && !hasZipFile) {
                validationResponse = ResponseEntity.badRequest().body(Map.of(
                    "error", "You must provide either gitUrl or a ZIP file"
                ));
            }
            
            if (validationResponse != null) {
                return validationResponse;
            }

            Project project = hasGitUrl 
                ? projectService.initializeProjectFromGit(name, gitUrl.trim())
                : projectService.initializeProjectFromZip(name, sourceZip);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProjectResponse.fromProject(project, ContainerStatus.STOPPED));
        } catch (Exception e) {
            log.error("Error creating project: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to create project",
                "details", e.getMessage()
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
                    .get(30, TimeUnit.SECONDS);
            return ResponseEntity.ok(ContainerResponse.fromContainerInfo(containerInfo, "start"));
        } catch (Exception e) {
            return e instanceof AccessDeniedException
                    ? ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                    : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
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
        } catch (Exception e) {
            return e instanceof AccessDeniedException
                    ? ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                    : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
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
        } catch (Exception e) {
            return e instanceof AccessDeniedException
                    ? ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                    : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ContainerResponse.error("restart", e.getMessage()));
        }
    }

    @Operation(
        summary = "Get project details",
        description = "Get detailed information about a specific project. Use statusOnly=true to get only the container status."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Project details retrieved successfully",
        content = @Content(schema = @Schema(oneOf = {ProjectDetailResponse.class, ContainerResponse.class}))
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
    @GetMapping("/{id}/details")
    public ResponseEntity<?> getProjectDetails(
            @PathVariable String id,
            @Parameter(description = "If true, returns only the container status without additional metrics")
            @RequestParam(required = false, defaultValue = "false") boolean statusOnly) {
        try {
            var project = projectService.getProjectById(id);
            checkProjectOwnership(project);

            var status = dockerService.getProjectStatus(id);
            
            if (statusOnly)
                return ResponseEntity.ok(ContainerResponse.fromStatus(status));
            
            var cpuUsage = "0";
            var memoryUsage = "0";
            String serverUrl = null;
            var autoRestartEnabled = project.isAutoRestartEnabled();
            
            if (status == ContainerStatus.RUNNING) {
                try {
                    cpuUsage = dockerService.getContainerMetrics(project)[0];
                    memoryUsage = dockerService.getContainerMetrics(project)[1];
                    serverUrl = dockerService.getProjectUrl(id);
                } catch (Exception e) {
                    log.warn("Failed to get container metrics for project {}: {}", id, e.getMessage());
                }
            }
            
            var technology = projectService.getProjectTechnology(id);

            return ResponseEntity.ok(ProjectDetailResponse.fromProject(
                project, status, cpuUsage, memoryUsage, serverUrl, technology, autoRestartEnabled));
        } catch (Exception e) {
            if (e instanceof AccessDeniedException) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            log.error("Error getting project details for {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get project details", 
                                 "details", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
    @Operation(
            summary = "Configure auto-restart for a project",
            description = "Enable or disable automatic restart for the specified project"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Auto-restart configuration updated successfully"
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User doesn't own this project"
    )
    @SecurityRequirement(name = "bearer-key")
    @PostMapping("/{id}/auto-restart")
    public ResponseEntity<?> configureAutoRestart(
            @PathVariable String id,
            @RequestBody AutoRestartRequest request) {
        try {
            var project = projectService.getProjectById(id);
            checkProjectOwnership(project);

            dockerService.configureAutoRestart(id, request.isEnabled());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Auto-restart " + (request.isEnabled() ? "enabled" : "disabled"),
                    "projectId", id,
                    "autoRestartEnabled", request.isEnabled()
            ));
        } catch (Exception e) {
            return e instanceof AccessDeniedException
                    ? ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                    : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to configure auto-restart",
                            "details", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
}