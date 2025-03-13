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
import java.io.IOException;

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
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with output: " + output);
        }

        return output.toString().trim();
    }

    private void buildImage(Project project) throws IOException, InterruptedException {
        var projectPath = fileService.getProjectStoragePath(project);
        var commandArgs = new String[]{
            "docker", "build", 
            "-t", getContainerName(project), 
            projectPath
        };
        var processBuilder = new ProcessBuilder(commandArgs)
            .directory(new File(projectPath))
            .redirectErrorStream(true);
        
        try {
            executeDockerCommand(processBuilder);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to build image for project " + project.getId(), e);
        }
    }

    private void stopAndRemoveContainer(Project project) throws IOException, InterruptedException {
        var stopCommand = new String[]{
            "docker", "rm", "-f", 
            getContainerName(project)
        };
        var processBuilder = new ProcessBuilder(stopCommand)
            .redirectErrorStream(true);
        
        try {
            executeDockerCommand(processBuilder);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to stop and remove container for project " + project.getId(), e);
        }
    }

    private String startContainer(Project project) throws IOException, InterruptedException {
        var runCommand = new String[]{
            "docker", "run", "-d",
            "-p", "0:3000",
            "--name", getContainerName(project),
            "--restart", "on-failure:3",
            getContainerName(project)
        };
        
        var processBuilder = new ProcessBuilder(runCommand)
            .redirectErrorStream(true);
        
        return executeDockerCommand(processBuilder);
    }

    private String getContainerPort(Project project) throws IOException, InterruptedException {
        var inspectCommand = new String[]{
            "docker", "inspect",
            "--format", "{{(index (index .NetworkSettings.Ports \"3000/tcp\") 0).HostPort}}",
            getContainerName(project)
        };
        
        var processBuilder = new ProcessBuilder(inspectCommand)
            .redirectErrorStream(true);
        
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

                if (!Files.exists(dockerfilePath)) {
                    return ContainerInfo.builder()
                            .status(ContainerStatus.ERROR)
                            .build();
                }

                buildImage(project);
                stopAndRemoveContainer(project);
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
                stopAndRemoveContainer(project);
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
                var restartCommand = new String[]{"docker", "restart", getContainerName(project)};
                var processBuilder = new ProcessBuilder(restartCommand)
                    .redirectErrorStream(true);
                
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
        return metrics.split(";");
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