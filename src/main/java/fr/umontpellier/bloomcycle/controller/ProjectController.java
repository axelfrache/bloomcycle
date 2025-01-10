package fr.umontpellier.bloomcycle.controller;

import fr.umontpellier.bloomcycle.service.FileService;
import fr.umontpellier.bloomcycle.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

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
}