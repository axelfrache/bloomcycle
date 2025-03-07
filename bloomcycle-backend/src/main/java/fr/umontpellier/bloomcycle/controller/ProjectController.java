package fr.umontpellier.bloomcycle.controller;

import fr.umontpellier.bloomcycle.dto.ProjectResponse;
import fr.umontpellier.bloomcycle.model.Project;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Project management endpoints")
public class ProjectController {

    private final ProjectService projectService;

    @Autowired
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
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
}