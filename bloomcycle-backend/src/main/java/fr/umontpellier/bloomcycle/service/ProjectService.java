package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.repository.ProjectRepository;
import fr.umontpellier.bloomcycle.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import fr.umontpellier.bloomcycle.service.ProjectTypeAnalyzer.TechnologyStack;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final GitService gitService;
    private final ProjectTypeAnalyzer projectAnalyzer;
    private final DockerComposeGenerator dockerComposeGenerator;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository, FileService fileService, GitService gitService, ProjectTypeAnalyzer projectAnalyzer, DockerComposeGenerator dockerComposeGenerator) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.fileService = fileService;
        this.gitService = gitService;
        this.projectAnalyzer = projectAnalyzer;
        this.dockerComposeGenerator = dockerComposeGenerator;
    }

    public List<Project> getProjectsByUserId(Long userId) {
        return projectRepository.findByOwnerId(userId);
    }

    public Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + projectId));
    }

    private Project createProject(String projectName, Long userId) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        var project = new Project();
        project.setName(projectName);
        project.setOwner(user);
        project.setStatus("Initialized");
        
        return projectRepository.save(project);
    }

    private void analyzeAndSetupProject(String projectPath) {
        try {
            var technology = projectAnalyzer.analyzeTechnology(projectPath);
            var existingDockerCompose = projectAnalyzer.findContainerization(projectPath);
            
            if (existingDockerCompose.isEmpty() && technology != TechnologyStack.UNKNOWN) {
                dockerComposeGenerator.generateDockerCompose(projectPath, technology);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error analyzing project: " + e.getMessage(), e);
        }
    }

    public Project initializeProjectFromGit(String projectName, String gitUrl, Long userId) {
        try {
            var project = createProject(projectName, userId);
            var projectPath = fileService.getProjectStoragePath(project.getId());
            
            Files.createDirectories(Paths.get(projectPath));
            gitService.cloneRepository(gitUrl, projectPath);
            
            analyzeAndSetupProject(projectPath);

            return project;
        } catch (Exception e) {
            throw new RuntimeException("Error initializing project: " + e.getMessage(), e);
        }
    }

    public Project initializeProjectFromZip(String projectName, MultipartFile sourceZip, Long userId) {
        try {
            var project = createProject(projectName, userId);
            var projectPath = fileService.getProjectStoragePath(project.getId());
            var targetPath = Paths.get(projectPath);
            
            Files.createDirectories(targetPath);
            fileService.extractZipFile(sourceZip, targetPath);
            
            analyzeAndSetupProject(projectPath);

            return project;
        } catch (Exception e) {
            throw new RuntimeException("Error initializing project from ZIP: " + e.getMessage(), e);
        }
    }

    public void deleteProject(Long projectId) {
        try {
            var project = getProjectById(projectId);
            var projectPath = fileService.getProjectStoragePath(projectId);
            
            fileService.deleteProjectDirectory(projectPath);
            
            projectRepository.delete(project);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting project: " + e.getMessage(), e);
        }
    }
}