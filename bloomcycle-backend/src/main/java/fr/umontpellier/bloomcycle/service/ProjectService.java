package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.model.User;
import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.repository.ProjectRepository;

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
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final FileService fileService;
    private final GitService gitService;
    private final ProjectTypeAnalyzer projectAnalyzer;

    public Project getProjectById(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }

    public List<Project> getProjectsByUser(User user) {
        return projectRepository.findByOwner(user);
    }

    public List<Project> getCurrentUserProjects() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var currentUser = (User) authentication.getPrincipal();
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
    }

    private void analyzeAndSetupProject(Project project) throws IOException {
        try {
            var projectPath = fileService.getProjectStoragePath(project);
            var dockerfilePath = Path.of(projectPath, "Dockerfile");

            if (Files.exists(dockerfilePath)) {
                log.info("Using existing Dockerfile for project {}", project.getId());
                return;
            }

            try {
                var technology = projectAnalyzer.analyzeTechnology(projectPath);
                generateDockerfile(projectPath, technology);
                log.info("Generated Dockerfile for {} project {}", technology, project.getId());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Project type not recognized. Please include a Dockerfile in your project sources."
                );
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
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

    public void deleteProject(String projectId) {
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

    public String getProjectTechnology(String projectId) {
        var project = getProjectById(projectId);
        var projectPath = fileService.getProjectStoragePath(project);
        return projectAnalyzer.analyzeTechnology(projectPath).name();
    }

    public void updateAutoRestartSetting(String projectId, boolean enabled) {
        Project project = getProjectById(projectId);
        project.setAutoRestartEnabled(enabled);
        projectRepository.save(project);
        log.info("Updated auto-restart setting for project {} to {}", projectId, enabled);
    }
}