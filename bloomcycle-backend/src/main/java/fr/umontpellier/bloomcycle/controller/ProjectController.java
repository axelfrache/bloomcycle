package fr.umontpellier.bloomcycle.controller;

import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.service.FileService;
import fr.umontpellier.bloomcycle.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final FileService fileService;

    @Autowired
    public ProjectController(ProjectService projectService, FileService fileService) {
        this.projectService = projectService;
        this.fileService = fileService;
    }

    @PostMapping("/initialize")
    public ResponseEntity<Project> initializeProject(@RequestParam String projectName, @RequestParam String sourcePath, @RequestParam Long userId) {
        try {
            var project = projectService.initializeProject(projectName, sourcePath, userId);
            return ResponseEntity.ok(project);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Project>> getProjectsByUserId(@PathVariable Long userId) {
        try {
            var projects = projectService.getProjectsByUserId(userId);
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{projectId}/files/check")
    public ResponseEntity<Map<String, Object>> checkProjectFiles(@PathVariable Long projectId) {
        try {
            var project = projectService.getProjectById(projectId);
            var projectPath = fileService.getProjectStoragePath(projectId);
            var projectDir = new File(projectPath);

            Map<String, Object> response = new HashMap<>();
            response.put("projectName", project.getName());
            response.put("storagePath", projectPath);
            response.put("exists", projectDir.exists());

            if (projectDir.exists()) {
                var files = new ArrayList<>();
                for (File file : Objects.requireNonNull(projectDir.listFiles())) {
                    Map<String, String> fileInfo = new HashMap<>();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("size", String.valueOf(file.length()));
                    fileInfo.put("path", file.getAbsolutePath());
                    files.add(fileInfo);
                }
                response.put("files", files);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}