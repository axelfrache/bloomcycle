package fr.umontpellier.bloomcycle.controller;

import fr.umontpellier.bloomcycle.dto.ProjectResponse;
import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.service.DockerService;
import fr.umontpellier.bloomcycle.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Project management endpoints")
public class ProjectController {

    private final ProjectService projectService;
    private final DockerService dockerService;

    @Autowired
    public ProjectController(ProjectService projectService, DockerService dockerService) {
        this.projectService = projectService;
        this.dockerService = dockerService;
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
            @Parameter(description = "User ID") @RequestParam("userId") Long userId,
            @Parameter(description = "Git repository URL") @RequestParam(value = "gitUrl", required = false) String gitUrl,
            @Parameter(description = "Source code as ZIP file") @RequestParam(value = "sourceZip", required = false) MultipartFile sourceZip) {
        try {
            Project project;
            if (gitUrl != null && !gitUrl.isEmpty()) {
                project = projectService.initializeProjectFromGit(name, gitUrl, userId);
            } else if (sourceZip != null && !sourceZip.isEmpty()) {
                project = projectService.initializeProjectFromZip(name, sourceZip, userId);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "You must provide either gitUrl or a ZIP file"
                ));
            }
            return ResponseEntity.ok(ProjectResponse.fromProject(project));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "details", e.getCause() != null ? e.getCause().getMessage() : "No additional details"
            ));
        }
    }

    @Operation(summary = "Get projects by user ID")
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ProjectResponse>> getUserProjects(
            @Parameter(description = "ID of the user") @PathVariable Long userId) {
        try {
            var projects = projectService.getProjectsByUserId(userId)
                    .stream()
                    .map(ProjectResponse::fromProject)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @Operation(summary = "Get project by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable Long id) {
        try {
            var project = projectService.getProjectById(id);
            return ResponseEntity.ok(ProjectResponse.fromProject(project));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return null; // Placeholder return, actual implementation needed
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
    public ResponseEntity<Map<String, String>> startProject(
            @Parameter(description = "Project ID") @PathVariable Long id) {
        try {
            projectService.getProjectById(id); // Vérifie que le projet existe
            dockerService.startProject(id)
                .thenAccept(status -> {
                    if (status == DockerService.ContainerStatus.ERROR) {
                        throw new RuntimeException("Failed to start project");
                    }
                });
            return ResponseEntity.accepted()
                .body(Map.of(
                    "message", "Project start initiated",
                    "status", "PENDING"
                ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Failed to start project",
                    "message", e.getMessage()
                ));
        }
    }

    @Operation(summary = "Stop a project's containers")
    @ApiResponse(responseCode = "202", description = "Project stop initiated")
    @ApiResponse(responseCode = "404", description = "Project not found")
    @ApiResponse(responseCode = "500", description = "Error stopping project")
    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, String>> stopProject(
            @Parameter(description = "Project ID") @PathVariable Long id) {
        try {
            projectService.getProjectById(id);
            dockerService.stopProject(id)
                .thenAccept(status -> {
                    if (status == DockerService.ContainerStatus.ERROR) {
                        throw new RuntimeException("Failed to stop project");
                    }
                });
            return ResponseEntity.accepted()
                .body(Map.of(
                    "message", "Project stop initiated",
                    "status", "PENDING"
                ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Failed to stop project",
                    "message", e.getMessage()
                ));
        }
    }

    @Operation(summary = "Restart a project's containers")
    @ApiResponse(responseCode = "202", description = "Project restart initiated")
    @ApiResponse(responseCode = "404", description = "Project not found")
    @ApiResponse(responseCode = "500", description = "Error restarting project")
    @PostMapping("/{id}/restart")
    public ResponseEntity<Map<String, String>> restartProject(
            @Parameter(description = "Project ID") @PathVariable Long id) {
        try {
            projectService.getProjectById(id);
            dockerService.restartProject(id)
                .thenAccept(status -> {
                    if (status == DockerService.ContainerStatus.ERROR) {
                        throw new RuntimeException("Failed to restart project");
                    }
                });
            return ResponseEntity.accepted()
                .body(Map.of(
                    "message", "Project restart initiated",
                    "status", "PENDING"
                ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Failed to restart project",
                    "message", e.getMessage()
                ));
        }
    }

    @Operation(summary = "Get project containers status")
    @ApiResponse(
        responseCode = "200", 
        description = "Project status retrieved successfully",
        content = @Content(schema = @Schema(implementation = DockerService.ContainerStatus.class))
    )
    @ApiResponse(responseCode = "404", description = "Project not found")
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> getProjectStatus(
            @Parameter(description = "Project ID") @PathVariable Long id) {
        try {
            projectService.getProjectById(id);
            var status = dockerService.getProjectStatus(id);
            return ResponseEntity.ok(Map.of(
                "status", status.toString(),
                "message", String.format("Project containers are %s", status.toString().toLowerCase())
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Failed to get project status",
                    "message", e.getMessage()
                ));
        }
    }
}