package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.model.File;
import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.repository.ProjectRepository;
import fr.umontpellier.bloomcycle.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository, FileService fileService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.fileService = fileService;
    }

    public List<File> getProjectFiles(Long projectId) {
        var project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        return project.getFiles();
    }

    public Project initializeProject(String projectName, String sourcePath, Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var project = new Project();
        project.setName(projectName);
        project.setOwner(user);
        project.setStatus("Initialized");
        project = projectRepository.save(project);

        fileService.uploadSourcesToProject(project.getId(), sourcePath);

        return project;
    }

    public List<Project> getProjectsByUserId(Long userId) {
        return projectRepository.findByOwnerId(userId);
    }

    public Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + projectId));
    }
}