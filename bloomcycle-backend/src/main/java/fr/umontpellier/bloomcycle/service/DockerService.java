package fr.umontpellier.bloomcycle.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

import fr.umontpellier.bloomcycle.model.container.ContainerStatus;
import fr.umontpellier.bloomcycle.model.container.ContainerOperation;
import fr.umontpellier.bloomcycle.model.container.ContainerInfo;
import fr.umontpellier.bloomcycle.model.Project;

@Service
@RequiredArgsConstructor
public class DockerService {
    
    @Value("${app.storage.path}")
    private String storagePath;

    @Value("${app.server.host:localhost}")
    private String serverHost;

    private final ExecutorService dockerExecutor = Executors.newFixedThreadPool(10);
    private final FileService fileService;
    private final ProjectService projectService;
    
    private static final String PROJECT_STATE_DIR = "project_states";

    private String getContainerName(Project project) {
        return "project-" + project.getId();
    }

    public CompletableFuture<ContainerInfo> executeOperation(String projectId, ContainerOperation operation) {
        return switch (operation) {
            case START -> startProject(projectId);
            case STOP -> stopProject(projectId);
            case RESTART -> restartProject(projectId);
        };
    }

    private String executeDockerCommand(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        var process = processBuilder.start();
        var output = new StringBuilder();
        
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        var exitCode = process.waitFor();
        if (exitCode != 0)
            throw new RuntimeException("Command failed with output: " + output);

        return output.toString().trim();
    }

    private void buildImage(Project project) throws IOException, InterruptedException {
        var projectPath = fileService.getProjectStoragePath(project);
        var containerName = getContainerName(project);
        var commandArgs = List.of(
            "docker", "build", 
            "--cache-from", containerName,
            "-t", containerName, 
            projectPath
        );
        
        var processBuilder = new ProcessBuilder(commandArgs)
            .directory(new File(projectPath))
            .redirectErrorStream(true);
        
        try {
            executeDockerCommand(processBuilder);
            saveProjectState(project);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to build image for project " + project.getId(), e);
        }
    }

    private void stopContainer(Project project) throws IOException, InterruptedException {
        var containerName = getContainerName(project);
        var processBuilder = new ProcessBuilder(
            "docker", "rm", "-f", containerName
        ).redirectErrorStream(true);
        
        try {
            executeDockerCommand(processBuilder);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to stop and remove container for project " + project.getId(), e);
        }
    }

    private void startContainer(Project project) throws IOException, InterruptedException {
        var containerName = getContainerName(project);
        var processBuilder = new ProcessBuilder(
            "docker", "run", "-d",
            "-p", "0:3000",
            "--name", containerName,
            "--restart", "on-failure:3",
            containerName
        ).redirectErrorStream(true);
        
        executeDockerCommand(processBuilder);
    }

    private String getContainerPort(Project project) throws IOException, InterruptedException {
        var containerName = getContainerName(project);
        var processBuilder = new ProcessBuilder(
            "docker", "inspect",
            "--format", "{{(index (index .NetworkSettings.Ports \"3000/tcp\") 0).HostPort}}",
            containerName
        ).redirectErrorStream(true);
        
        return executeDockerCommand(processBuilder);
    }

    private String buildServerUrl(String port) {
        return String.format("http://%s:%s", serverHost, port);
    }

    private CompletableFuture<ContainerInfo> startProject(String projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var project = projectService.getProjectById(projectId);
                var projectPath = fileService.getProjectStoragePath(project);
                var dockerfilePath = Path.of(projectPath, "Dockerfile");

                if (!Files.exists(dockerfilePath))
                    return ContainerInfo.builder()
                            .status(ContainerStatus.ERROR)
                            .build();

                if (!imageExists(project) || shouldRebuildImage(project))
                    buildImage(project);
                
                stopContainer(project);
                startContainer(project);
                
                var hostPort = getContainerPort(project);
                var serverUrl = buildServerUrl(hostPort);

                return ContainerInfo.builder()
                        .status(ContainerStatus.RUNNING)
                        .serverUrl(serverUrl)
                        .build();
            } catch (Exception e) {
                return ContainerInfo.builder()
                        .status(ContainerStatus.ERROR)
                        .build();
            }
        }, dockerExecutor);
    }

    private CompletableFuture<ContainerInfo> stopProject(String projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var project = projectService.getProjectById(projectId);
                stopContainer(project);
                return ContainerInfo.builder()
                        .status(ContainerStatus.STOPPED)
                        .build();
            } catch (Exception e) {
                return ContainerInfo.builder()
                        .status(ContainerStatus.ERROR)
                        .build();
            }
        }, dockerExecutor);
    }

    private CompletableFuture<ContainerInfo> restartProject(String projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var project = projectService.getProjectById(projectId);
                var containerName = getContainerName(project);
                var processBuilder = new ProcessBuilder(
                    "docker", "restart", containerName
                ).redirectErrorStream(true);
                
                executeDockerCommand(processBuilder);
                
                var hostPort = getContainerPort(project);
                var serverUrl = buildServerUrl(hostPort);

                return ContainerInfo.builder()
                        .status(ContainerStatus.RUNNING)
                        .serverUrl(serverUrl)
                        .build();
            } catch (Exception e) {
                return ContainerInfo.builder()
                        .status(ContainerStatus.ERROR)
                        .build();
            }
        }, dockerExecutor);
    }

    private boolean imageExists(Project project) throws IOException, InterruptedException {
        var containerName = getContainerName(project);
        var processBuilder = new ProcessBuilder(
            "docker", "image", "inspect", containerName
        ).redirectErrorStream(true);
        
        try {
            executeDockerCommand(processBuilder);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean shouldRebuildImage(Project project) {
        var projectPath = fileService.getProjectStoragePath(project);
        var stateFilePath = getProjectStatePath(project);
        
        if (!Files.exists(stateFilePath))
            return true;
        
        try {
            var currentHash = calculateProjectHash(projectPath);
            var savedHash = Files.readString(stateFilePath);
            
            return !currentHash.equals(savedHash);
        } catch (Exception e) {
            return true;
        }
    }

    private void saveProjectState(Project project) throws IOException {
        var projectPath = fileService.getProjectStoragePath(project);
        var stateFilePath = getProjectStatePath(project);
        
        Files.createDirectories(stateFilePath.getParent());
        var projectHash = calculateProjectHash(projectPath);
        Files.writeString(stateFilePath, projectHash);
    }

    private String calculateProjectHash(String projectPath) throws IOException {
        var dockerfilePath = Path.of(projectPath, "Dockerfile");
        
        if (!Files.exists(dockerfilePath))
            throw new IOException("Dockerfile not found in project");
        
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            
            var dockerfileContent = Files.readString(dockerfilePath);
            digest.update(dockerfileContent.getBytes(StandardCharsets.UTF_8));
            
            Predicate<Path> isRelevantFile = p -> {
                var fileName = p.getFileName().toString();
                var pathStr = p.toString();
                return !fileName.startsWith(".") && 
                       !pathStr.contains("/node_modules/") && 
                       !pathStr.contains("/target/") && 
                       !pathStr.contains("/.git/");
            };
            
            Predicate<String> isCriticalFile = fileName -> 
                List.of("package.json", "package-lock.json", "pom.xml", "build.gradle")
                    .contains(fileName);
            
            try (var pathStream = Files.walk(Paths.get(projectPath))) {
                pathStream
                    .filter(Files::isRegularFile)
                    .filter(isRelevantFile)
                    .forEach(p -> {
                        try {
                            var attrs = Files.readAttributes(p, BasicFileAttributes.class);
                            var fileInfo = p + "|" + 
                                          attrs.lastModifiedTime().toMillis() + "|" + 
                                          attrs.size();
                            digest.update(fileInfo.getBytes(StandardCharsets.UTF_8));
                            
                            var fileName = p.getFileName().toString();
                            if (isCriticalFile.test(fileName)) {
                                var content = Files.readString(p);
                                digest.update(content.getBytes(StandardCharsets.UTF_8));
                            }
                        } catch (IOException e) {
                            System.err.println("Erreur lors du traitement du fichier " + p + ": " + e.getMessage());
                        }
                    });
            }
            
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Failed to calculate project hash", e);
        }
    }

    private Path getProjectStatePath(Project project) {
        return Paths.get(storagePath, PROJECT_STATE_DIR, project.getId() + ".hash");
    }

    public ContainerStatus getProjectStatus(String projectId) {
        try {
            var project = projectService.getProjectById(projectId);
            var processBuilder = new ProcessBuilder(
                "docker", "ps",
                "--filter", "name=" + getContainerName(project),
                "--format", "{{.Status}}"
            ).redirectErrorStream(true);
            
            var output = executeDockerCommand(processBuilder);
            return output.toLowerCase().contains("up")
                ? ContainerStatus.RUNNING 
                : ContainerStatus.STOPPED;
        } catch (Exception e) {
            return ContainerStatus.ERROR;
        }
    }

    public String[] getContainerMetrics(Project project) throws IOException, InterruptedException {
        var runCommand = new String[]{
                "docker", "stats",
                "--no-stream",
                "--format", "{{.CPUPerc}};{{.MemPerc}}",
                getContainerName(project)
        };

        var processBuilder = new ProcessBuilder(runCommand)
                .redirectErrorStream(true);

        String metrics = executeDockerCommand(processBuilder);
        String[] values = metrics.split(";");
        return new String[]{
                values[0].replace("%", ""),
                values[1].replace("%", "")
        };
    }

    public String getProjectLogs(Project project) throws IOException, InterruptedException {
        var runCommand = new String[]{
                "docker", "logs", getContainerName(project)
        };

        ProcessBuilder processBuilder = new ProcessBuilder(runCommand)
                .redirectErrorStream(true);

        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to get project logs. Exit code: " + exitCode);
        }

        return output.toString();
    }

    public String getProjectUrl(String projectId) {
        try {
            var project = projectService.getProjectById(projectId);
            var hostPort = getContainerPort(project);
            return buildServerUrl(hostPort);
        } catch (Exception e) {
            return null;
        }
    }
}