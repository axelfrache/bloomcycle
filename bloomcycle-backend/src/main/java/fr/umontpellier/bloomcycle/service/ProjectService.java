package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.model.User;
import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.repository.ProjectRepository;
import fr.umontpellier.bloomcycle.repository.UserRepository;
import fr.umontpellier.bloomcycle.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import fr.umontpellier.bloomcycle.service.ProjectTypeAnalyzer.TechnologyStack;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final GitService gitService;
    private final ProjectTypeAnalyzer projectAnalyzer;
    private final DockerComposeGenerator dockerComposeGenerator;

    public List<Project> getProjectsByUserId(Long userId) {
        return projectRepository.findByOwnerId(userId);
    }

    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }

    public List<Project> getProjectsByUser(User user) {
        return projectRepository.findByOwner(user);
    }

    public List<Project> getCurrentUserProjects() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var currentUser = (User) authentication.getPrincipal();
        log.info("Getting projects for user: {}", currentUser.getEmail());
        return getProjectsByUser(currentUser);
    }

    private Project createProject(String projectName, Long userId) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        var project = new Project();
        project.setName(projectName);
        project.setOwner(user);
        
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

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }
}