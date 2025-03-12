package fr.umontpellier.bloomcycle.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

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

@Service
@Slf4j
public class DockerService {
    
    @Value("${app.storage.path}")
    private String storagePath;

    @Value("${app.server.host:localhost}")
    private String serverHost;

    private final ExecutorService dockerExecutor = Executors.newFixedThreadPool(10);

    public CompletableFuture<ContainerInfo> executeOperation(Long projectId, ContainerOperation operation) {
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

    private void buildImage(String projectPath, Long projectId) throws IOException, InterruptedException {
        var commandArgs = new String[]{"docker", "build", "-t", "project-" + projectId, projectPath};
        var processBuilder = new ProcessBuilder(commandArgs)
            .directory(new File(projectPath))
            .redirectErrorStream(true);
        
        try {
            executeDockerCommand(processBuilder);
        } catch (RuntimeException e) {
            log.error("Docker build failed for project {}", projectId);
            throw e;
        }
    }

    private void stopAndRemoveContainer(Long projectId) throws IOException, InterruptedException {
        var stopCommand = new String[]{"docker", "rm", "-f", "project-" + projectId};
        var processBuilder = new ProcessBuilder(stopCommand)
            .redirectErrorStream(true);
        
        try {
            executeDockerCommand(processBuilder);
        } catch (RuntimeException e) {
            // Ignorer l'erreur si le conteneur n'existe pas
            log.debug("Container might not exist for project {}", projectId);
        }
    }

    private String startContainer(Long projectId) throws IOException, InterruptedException {
        ensureUniqueContainerName(projectId);
        
        var runCommand = new String[]{
            "docker", "run", "-d",
            "-p", "0:3000",
            "--name", "project-" + projectId,
            "--restart", "on-failure:3",
            "project-" + projectId
        };
        
        var processBuilder = new ProcessBuilder(runCommand)
            .redirectErrorStream(true);
        
        return executeDockerCommand(processBuilder);
    }

    private String getContainerPort(Long projectId) throws IOException, InterruptedException {
        var inspectCommand = new String[]{
            "docker", "inspect",
            "--format", "{{(index (index .NetworkSettings.Ports \"3000/tcp\") 0).HostPort}}",
            "project-" + projectId
        };
        
        var processBuilder = new ProcessBuilder(inspectCommand)
            .redirectErrorStream(true);
        
        return executeDockerCommand(processBuilder);
    }

    private String buildServerUrl(String port) {
        return String.format("http://%s:%s", serverHost, port);
    }

    private CompletableFuture<ContainerInfo> startProject(Long projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var projectPath = Path.of(storagePath, "projects", projectId.toString()).toString();
                var dockerfilePath = Path.of(projectPath, "Dockerfile");

                if (!Files.exists(dockerfilePath)) {
                    log.error("Dockerfile not found for project {}", projectId);
                    return ContainerInfo.builder()
                            .status(ContainerStatus.ERROR)
                            .build();
                }

                buildImage(projectPath, projectId);
                stopAndRemoveContainer(projectId);
                startContainer(projectId);
                
                var hostPort = getContainerPort(projectId);
                var serverUrl = buildServerUrl(hostPort);
                log.info("Project {} is running at: {}", projectId, serverUrl);

                return ContainerInfo.builder()
                        .status(ContainerStatus.RUNNING)
                        .serverUrl(serverUrl)
                        .build();
            } catch (Exception e) {
                log.error("Error starting project {}", projectId, e);
                return ContainerInfo.builder()
                        .status(ContainerStatus.ERROR)
                        .build();
            }
        }, dockerExecutor);
    }

    private CompletableFuture<ContainerInfo> stopProject(Long projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                stopAndRemoveContainer(projectId);
                return ContainerInfo.builder()
                        .status(ContainerStatus.STOPPED)
                        .build();
            } catch (Exception e) {
                log.error("Error stopping project {}", projectId, e);
                return ContainerInfo.builder()
                        .status(ContainerStatus.ERROR)
                        .build();
            }
        }, dockerExecutor);
    }

    private CompletableFuture<ContainerInfo> restartProject(Long projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var restartCommand = new String[]{"docker", "restart", "project-" + projectId};
                var processBuilder = new ProcessBuilder(restartCommand)
                    .redirectErrorStream(true);
                
                executeDockerCommand(processBuilder);
                
                var hostPort = getContainerPort(projectId);
                var serverUrl = buildServerUrl(hostPort);
                log.info("Project {} is running at: {}", projectId, serverUrl);

                return ContainerInfo.builder()
                        .status(ContainerStatus.RUNNING)
                        .serverUrl(serverUrl)
                        .build();
            } catch (Exception e) {
                log.error("Error restarting project {}", projectId, e);
                return ContainerInfo.builder()
                        .status(ContainerStatus.ERROR)
                        .build();
            }
        }, dockerExecutor);
    }

    public ContainerStatus getProjectStatus(Long projectId) {
        try {
            var processBuilder = new ProcessBuilder(
                "docker", "ps",
                "--filter", "name=project-" + projectId,
                "--format", "{{.Status}}"
            ).redirectErrorStream(true);
            
            var output = executeDockerCommand(processBuilder);
            return output.toLowerCase().contains("up")
                ? ContainerStatus.RUNNING 
                : ContainerStatus.STOPPED;
        } catch (Exception e) {
            log.error("Error checking status for project {}", projectId, e);
            return ContainerStatus.ERROR;
        }
    }

    public String getProjectUrl(Long projectId) {
        try {
            var hostPort = getContainerPort(projectId);
            return buildServerUrl(hostPort);
        } catch (Exception e) {
            log.error("Error getting project URL for project {}", projectId, e);
            return null;
        }
    }

    private void ensureUniqueContainerName(Long projectId) throws IOException, InterruptedException {
        var checkCommand = new String[]{
            "docker", "ps", "-a",
            "--filter", "name=project-" + projectId,
            "--format", "{{.Names}}"
        };
        
        var processBuilder = new ProcessBuilder(checkCommand)
            .redirectErrorStream(true);
        
        var output = executeDockerCommand(processBuilder);
        if (!output.isEmpty()) {
            stopAndRemoveContainer(projectId);
        }
    }
}