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
import java.nio.file.Path;
import java.io.IOException;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import fr.umontpellier.bloomcycle.service.ProjectTypeAnalyzer.TechnologyStack;
import fr.umontpellier.bloomcycle.exception.UnauthorizedAccessException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final GitService gitService;
    private final ProjectTypeAnalyzer projectAnalyzer;

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

    private Project createProject(String projectName) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var currentUser = (User) authentication.getPrincipal();

        var project = new Project();
        project.setName(projectName);
        project.setOwner(currentUser);

        return projectRepository.save(project);
    }

    private void generateDockerfile(String projectPath, TechnologyStack technology) throws IOException {
        log.info("Generating Dockerfile for project at path: {}", projectPath);
        String dockerfileContent = switch (technology) {
            case JAVA_MAVEN -> """
                FROM maven:3.8-openjdk-17
                WORKDIR /app
                COPY . .
                RUN mvn clean package
                CMD ["java", "-jar", "target/*.jar"]
                """;
            case NODEJS -> """
                FROM node:20-alpine
                WORKDIR /app
                COPY package*.json ./
                RUN npm install
                COPY . .
                EXPOSE 3000
                CMD ["npm", "start"]
                """;
            case PYTHON -> """
                FROM python:3.9
                WORKDIR /app
                COPY requirements.txt .
                RUN pip install -r requirements.txt
                COPY . .
                CMD ["python", "app.py"]
                """;
            default -> throw new IllegalArgumentException("Unknown project type");
        };

        var dockerfilePath = Path.of(projectPath, "Dockerfile");
        Files.writeString(dockerfilePath, dockerfileContent);
        log.info("Dockerfile generated successfully at: {}", dockerfilePath);
    }

    private void analyzeAndSetupProject(Project project) {
        try {
            var projectPath = fileService.getProjectStoragePath(project);
            var technology = projectAnalyzer.analyzeTechnology(projectPath);
            log.info("Technology detected: {}", technology);
            
            var dockerfilePath = Path.of(projectPath, "Dockerfile");
            if (!Files.exists(dockerfilePath)) {
                generateDockerfile(projectPath, technology);
            } else {
                log.info("Using existing Dockerfile from project");
            }
        } catch (Exception e) {
            log.error("Error analyzing project: {}", e.getMessage(), e);
            throw new RuntimeException("Error analyzing project: " + e.getMessage(), e);
        }
    }

    public Project initializeProjectFromGit(String projectName, String gitUrl) {
        try {
            var project = createProject(projectName);
            var projectPath = fileService.getProjectStoragePath(project);

            Files.createDirectories(Paths.get(projectPath));
            gitService.cloneRepository(gitUrl, projectPath);

            analyzeAndSetupProject(project);

            return project;
        } catch (Exception e) {
            log.error("Error initializing project: {}", e.getMessage());
            throw new RuntimeException("Error initializing project: " + e.getMessage(), e);
        }
    }

    public Project initializeProjectFromZip(String projectName, MultipartFile sourceZip) {
        try {
            var project = createProject(projectName);
            var projectPath = fileService.getProjectStoragePath(project);
            var targetPath = Paths.get(projectPath);

            Files.createDirectories(targetPath);
            fileService.extractZipFile(sourceZip, targetPath);

            analyzeAndSetupProject(project);

            return project;
        } catch (Exception e) {
            throw new RuntimeException("Error initializing project from ZIP: " + e.getMessage(), e);
        }
    }

    public void deleteProject(Long projectId) {
        try {
            var project = getProjectById(projectId);
            
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var currentUser = (User) authentication.getPrincipal();
            if (!project.getOwner().getId().equals(currentUser.getId())) {
                throw new UnauthorizedAccessException("You don't have permission to delete this project");
            }

            var projectPath = fileService.getProjectStoragePath(project);
            fileService.deleteProjectDirectory(projectPath);
            projectRepository.delete(project);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting project: " + e.getMessage(), e);
        }
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public boolean hasCustomDockerfile(Long projectId) {
        var project = getProjectById(projectId);
        var projectPath = fileService.getProjectStoragePath(project);
        return Files.exists(Path.of(projectPath, "Dockerfile"));
    }

    public String getProjectTechnology(Long projectId) {
        var project = getProjectById(projectId);
        var projectPath = fileService.getProjectStoragePath(project);
        return projectAnalyzer.analyzeTechnology(projectPath).name();
    }
}