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

@Service
@Slf4j
public class DockerService {
    
    @Value("${app.storage.path}")
    private String storagePath;

    private final ExecutorService dockerExecutor = Executors.newFixedThreadPool(10);

    public enum ContainerStatus {
        RUNNING,
        STOPPED,
        ERROR
    }

    private CompletableFuture<ContainerStatus> executeDockerCompose(Long projectId, String command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var projectPath = Path.of(storagePath, "projects", projectId.toString()).toString();
                var commandArgs = new String[]{"docker", "compose", "-f", 
                    projectPath + "/docker-compose.yml"};
                
                var fullCommand = concat(commandArgs, command.split(" "));
                
                var processBuilder = new ProcessBuilder(fullCommand);
                processBuilder.directory(new File(projectPath));
                processBuilder.redirectErrorStream(true);
                
                var process = processBuilder.start();
                
                var output = new StringBuilder();
                try (var reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                var exitCode = process.waitFor();
                
                if (exitCode != 0) {
                    log.error("Docker command failed for project {}: {}", projectId, output);
                    return ContainerStatus.ERROR;
                }

                return command.contains("down") ? ContainerStatus.STOPPED : ContainerStatus.RUNNING;
                
            } catch (Exception e) {
                log.error("Error executing docker command for project {}", projectId, e);
                return ContainerStatus.ERROR;
            }
        }, dockerExecutor);
    }

    private String[] concat(String[] first, String[] second) {
        var result = new String[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public ContainerStatus getProjectStatus(Long projectId) {
        try {
            var projectPath = Path.of(storagePath, "projects", projectId.toString()).toString();
            
            var processBuilder = new ProcessBuilder(
                "docker", "compose", "-f",
                projectPath + "/docker-compose.yml",
                "ps", "--format", "json"
            );
            
            processBuilder.directory(new File(projectPath));
            processBuilder.redirectErrorStream(true);
            var process = processBuilder.start();
            
            var output = new StringBuilder();
            try (var reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            var exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Status check failed for project {}", projectId);
                return ContainerStatus.ERROR;
            }

            return output.toString().toLowerCase().contains("running")
                ? ContainerStatus.RUNNING 
                : ContainerStatus.STOPPED;
            
        } catch (Exception e) {
            log.error("Error checking status for project {}", projectId, e);
            return ContainerStatus.ERROR;
        }
    }

    public CompletableFuture<ContainerStatus> startProject(Long projectId) {
        return executeDockerCompose(projectId, "up -d --build");
    }

    public CompletableFuture<ContainerStatus> stopProject(Long projectId) {
        return executeDockerCompose(projectId, "down");
    }

    public CompletableFuture<ContainerStatus> restartProject(Long projectId) {
        return executeDockerCompose(projectId, "restart");
    }
}