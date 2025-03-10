package fr.umontpellier.bloomcycle.controller;

import fr.umontpellier.bloomcycle.dto.ProjectResponse;
import fr.umontpellier.bloomcycle.dto.container.ContainerResponse;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Project management endpoints")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;
    private final DockerService dockerService;

    @PostMapping("/git")
    public ResponseEntity<Project> createProjectFromGit(
            @RequestParam String projectName,
            @RequestParam String gitUrl) {
        try {
            var project = projectService.initializeProjectFromGit(projectName, gitUrl);
            return ResponseEntity.ok(project);
        } catch (Exception e) {
            log.error("Error creating project from git: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/zip")
    public ResponseEntity<Project> createProjectFromZip(
            @RequestParam String projectName,
            @RequestParam("file") MultipartFile sourceZip) {
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
    @ApiResponse(responseCode = "400", description = "Invalid input")
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

    @Operation(summary = "Get current user's projects")
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

    @Operation(summary = "Get projects by user ID")
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ProjectResponse>> getUserProjects(
            @Parameter(description = "ID of the user") @PathVariable Long userId) {
        try {
            var projects = projectService.getProjectsByUserId(userId)
                    .stream()
                    .map(project -> {
                        var containerStatus = dockerService.getProjectStatus(project.getId());
                        return ProjectResponse.fromProject(project, containerStatus);
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            log.error("Error getting projects for user {}", userId, e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    @Operation(summary = "Get project by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable Long id) {
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

    @Operation(summary = "Delete a project")
    @ApiResponse(responseCode = "204", description = "Project successfully deleted")
    @ApiResponse(responseCode = "404", description = "Project not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "ID of the project to delete")
            @PathVariable Long id) {
        try {
            projectService.deleteProject(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Start a project's containers")
    @ApiResponse(responseCode = "202", description = "Project start initiated")
    @ApiResponse(responseCode = "404", description = "Project not found")
    @ApiResponse(responseCode = "500", description = "Error starting project")
    @PostMapping("/{id}/start")
    public ResponseEntity<ContainerResponse> startProject(@PathVariable Long id) {
        try {
            var project = projectService.getProjectById(id);
            checkProjectOwnership(project);

            dockerService.startProject(id);
            return ResponseEntity.ok(ContainerResponse.success("start"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ContainerResponse.error("start", e.getMessage()));
        }
    }

    @Operation(summary = "Stop a project's containers")
    @ApiResponse(responseCode = "202", description = "Project stop initiated")
    @ApiResponse(responseCode = "404", description = "Project not found")
    @ApiResponse(responseCode = "500", description = "Error stopping project")
    @PostMapping("/{id}/stop")
    public ResponseEntity<ContainerResponse> stopProject(@PathVariable Long id) {
        try {
            projectService.getProjectById(id);
            dockerService.executeOperation(id, ContainerOperation.STOP)
                    .thenAccept(status -> {
                        if (status == ContainerStatus.ERROR) {
                            throw new RuntimeException("Failed to stop project");
                        }
                    });
            return ResponseEntity.accepted()
                    .body(ContainerResponse.pending("stop"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ContainerResponse.error("stop", e.getMessage()));
        }
    }

    @Operation(summary = "Restart a project's containers")
    @ApiResponse(responseCode = "202", description = "Project restart initiated")
    @ApiResponse(responseCode = "404", description = "Project not found")
    @ApiResponse(responseCode = "500", description = "Error restarting project")
    @PostMapping("/{id}/restart")
    public ResponseEntity<ContainerResponse> restartProject(@PathVariable Long id) {
        try {
            projectService.getProjectById(id);
            dockerService.executeOperation(id, ContainerOperation.RESTART)
                    .thenAccept(status -> {
                        if (status == ContainerStatus.ERROR) {
                            throw new RuntimeException("Failed to restart project");
                        }
                    });
            return ResponseEntity.accepted()
                    .body(ContainerResponse.pending("restart"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ContainerResponse.error("restart", e.getMessage()));
        }
    }

    @Operation(summary = "Get project containers status")
    @ApiResponse(
            responseCode = "200",
            description = "Project status retrieved successfully",
            content = @Content(schema = @Schema(implementation = ContainerResponse.class))
    )
    @ApiResponse(responseCode = "404", description = "Project not found")
    @GetMapping("/{id}/status")
    public ResponseEntity<ContainerResponse> getProjectStatus(@PathVariable Long id) {
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
}