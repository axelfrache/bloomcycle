package fr.umontpellier.bloomcycle.controller;

import fr.umontpellier.bloomcycle.dto.ProjectResponse;
import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    @Autowired
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> createProject(
            @RequestParam("name") String name,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "gitUrl", required = false) String gitUrl,
            @RequestParam(value = "sourceZip", required = false) MultipartFile sourceZip) {
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

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ProjectResponse>> getProjectsByUserId(@PathVariable Long userId) {
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

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long projectId) {
        try {
            var project = projectService.getProjectById(projectId);
            return ResponseEntity.ok(ProjectResponse.fromProject(project));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}