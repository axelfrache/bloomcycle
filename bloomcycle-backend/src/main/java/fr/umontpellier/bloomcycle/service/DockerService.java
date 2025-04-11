package fr.umontpellier.bloomcycle.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    private static final Logger log = LoggerFactory.getLogger(DockerService.class);
    
    private static final String DOCKER_NETWORK = "bloom-cycle_bloomcycle-network";
    private static final String FALLBACK_NETWORK = "bloomcycle-network";

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
        System.out.println("Exécution de la commande: " + String.join(" ", processBuilder.command()));
        
        var process = processBuilder.start();
        var output = new StringBuilder();
        var error = new StringBuilder();
        
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
        }

        var exitCode = process.waitFor();
        System.out.println("Résultat de la commande (code: " + exitCode + "):");
        System.out.println("Output: " + output);
        
        if (exitCode != 0) {
            System.err.println("Erreur: " + error);
            throw new RuntimeException("Command failed with exit code " + exitCode + ". Output: " + output + ". Error: " + error);
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
            "docker", "stop", 
            getContainerName(project)
        };
        var processBuilder = new ProcessBuilder(stopCommand)
            .redirectErrorStream(true);
        
        try {
            executeDockerCommand(processBuilder);
            
            var rmCommand = new String[]{
                "docker", "rm", 
                getContainerName(project)
            };
            var rmBuilder = new ProcessBuilder(rmCommand)
                .redirectErrorStream(true);
            executeDockerCommand(rmBuilder);
        } catch (RuntimeException e) {
            var forceCommand = new String[]{
                "docker", "rm", "-f", 
                getContainerName(project)
            };
            var forceBuilder = new ProcessBuilder(forceCommand)
                .redirectErrorStream(true);
            executeDockerCommand(forceBuilder);
        }
    }

    private void ensureNetworkExists() {
        try {
            var checkCommand = new String[]{
                "docker", "network", "inspect", DOCKER_NETWORK
            };
            var checkBuilder = new ProcessBuilder(checkCommand).redirectErrorStream(true);
            
            try {
                executeDockerCommand(checkBuilder);
                System.out.println("Le réseau " + DOCKER_NETWORK + " existe déjà");
                return;
            } catch (Exception e) {
                var checkFallbackCommand = new String[]{
                    "docker", "network", "inspect", FALLBACK_NETWORK
                };
                var checkFallbackBuilder = new ProcessBuilder(checkFallbackCommand).redirectErrorStream(true);
                
                try {
                    executeDockerCommand(checkFallbackBuilder);
                    System.out.println("Le réseau de secours " + FALLBACK_NETWORK + " existe déjà");
                    return;
                } catch (Exception e2) {
                    System.out.println("Création du réseau Docker " + DOCKER_NETWORK);
                    var createCommand = new String[]{
                        "docker", "network", "create", DOCKER_NETWORK
                    };
                    var createBuilder = new ProcessBuilder(createCommand).redirectErrorStream(true);
                    executeDockerCommand(createBuilder);
                    System.out.println("Réseau Docker " + DOCKER_NETWORK + " créé avec succès");
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification/création du réseau Docker: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String startContainer(Project project) throws IOException, InterruptedException {
        ensureNetworkExists();
        
        String subdomain = "project-" + project.getId();
        String containerName = getContainerName(project);
        
        System.out.println("Démarrage du conteneur pour le projet " + project.getId() + " avec le nom " + containerName);
        
        var runCommand = new String[]{
            "docker", "run", "-d",
            "-p", "0:3000",
            "--name", containerName,
            "--network", DOCKER_NETWORK, // Connecter au réseau de Traefik
            "--label", "traefik.enable=true",
            "--label", "traefik.http.routers." + subdomain + ".rule=Host(`" + subdomain + ".bloomcycle.localhost`)",
            "--label", "traefik.http.routers." + subdomain + ".entrypoints=web",
            "--label", "traefik.http.services." + subdomain + ".loadbalancer.server.port=3000",
            "--restart", project.isAutoRestartEnabled() ? "unless-stopped" : "on-failure:3",
            containerName
        };
        
        System.out.println("Starting container with command: " + String.join(" ", runCommand));
        
        var processBuilder = new ProcessBuilder(runCommand)
            .redirectErrorStream(true);
        
        String result;
        try {
            result = executeDockerCommand(processBuilder);
            System.out.println("Container start result: " + result);
            
            try {
                var connectCommand = new String[]{
                    "docker", "network", "connect", DOCKER_NETWORK, containerName
                };
                var connectBuilder = new ProcessBuilder(connectCommand).redirectErrorStream(true);
                executeDockerCommand(connectBuilder);
                System.out.println("Conteneur connecté au réseau Traefik avec succès");
            } catch (Exception e) {
                // Ignorer l'erreur si le conteneur est déjà connecté au réseau
                System.out.println("Note: Le conteneur est peut-être déjà connecté au réseau: " + e.getMessage());
            }
            
            var inspectCommand = new String[]{
                "docker", "inspect", "-f", "{{.State.Running}}", containerName
            };
            var inspectBuilder = new ProcessBuilder(inspectCommand).redirectErrorStream(true);
            String runningState = executeDockerCommand(inspectBuilder).trim();
            
            if (!"true".equals(runningState)) {
                System.err.println("Le conteneur n'est pas en cours d'exécution après le démarrage!");
                var logsCommand = new String[]{
                    "docker", "logs", containerName
                };
                var logsBuilder = new ProcessBuilder(logsCommand).redirectErrorStream(true);
                String logs = executeDockerCommand(logsBuilder);
                System.err.println("Logs du conteneur: " + logs);
            } else {
                System.out.println("Le conteneur est en cours d'exécution: " + runningState);
            }
            
            return result;
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du conteneur: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private String getContainerPort(Project project) throws IOException, InterruptedException {
        String containerName = getContainerName(project);
        System.out.println("Récupération du port pour le conteneur: " + containerName);
        
        try {
            var inspectCommand = new String[]{
                "docker", "inspect", "-f", "{{.State.Running}}", containerName
            };
            var inspectBuilder = new ProcessBuilder(inspectCommand).redirectErrorStream(true);
            String runningState = "";
            
            try {
                runningState = executeDockerCommand(inspectBuilder).trim();
                System.out.println("État du conteneur: " + runningState);
            } catch (Exception e) {
                System.err.println("Impossible de vérifier l'état du conteneur: " + e.getMessage());
                return "3000";
            }
            
            if (!"true".equals(runningState)) {
                System.out.println("Le conteneur n'est pas en cours d'exécution, utilisation du port par défaut");
                return "3000";
            }
            
            var portCommand = new String[]{
                "docker", "port", containerName, "3000"
            };
            var portBuilder = new ProcessBuilder(portCommand).redirectErrorStream(true);
            String portMapping = executeDockerCommand(portBuilder).trim();
            System.out.println("Mapping de port: " + portMapping);
            
            if (portMapping != null && !portMapping.isEmpty()) {
                String[] parts = portMapping.split(":");
                if (parts.length > 1) {
                    String port = parts[parts.length - 1];
                    System.out.println("Port récupéré: " + port);
                    return port;
                }
            }
            
            System.out.println("Impossible de récupérer le port, utilisation du port par défaut");
            return "3000";
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du port: " + e.getMessage());
            e.printStackTrace();
            return "3000";
        }
    }

    private String buildServerUrl(String port, Project project) {
        String subdomain = "project-" + project.getId();
        
        String basePath = "";
        
        String projectName = project.getName() != null ? project.getName().toLowerCase().replace(" ", "-") : "";
        if (!projectName.isEmpty()) {
            if (projectName.contains("pokemon")) {
                basePath = "/pokemon-finder";
            } else {
                basePath = "/" + projectName;
            }
        }
        
        return String.format("http://%s.bloomcycle.localhost%s", subdomain, basePath);
    }
    
    private String getProjectIdFromContainerName(String containerName) {
        return containerName.substring("project-".length());
    }

    private CompletableFuture<ContainerInfo> startProject(String projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var project = projectService.getProjectById(projectId);
                var projectPath = fileService.getProjectStoragePath(project);
                var dockerfilePath = Path.of(projectPath, "Dockerfile");

                if (!Files.exists(dockerfilePath)) {
                    System.out.println("Dockerfile not found at: " + dockerfilePath);
                    return ContainerInfo.builder()
                            .status(ContainerStatus.ERROR)
                            .build();
                }

                try {
                    buildImage(project);
                } catch (Exception e) {
                    System.out.println("Error building image: " + e.getMessage());
                    e.printStackTrace();
                    return ContainerInfo.builder()
                            .status(ContainerStatus.ERROR)
                            .build();
                }
                
                try {
                    stopAndRemoveContainer(project);
                } catch (Exception e) {
                    System.out.println("Error stopping container: " + e.getMessage());
                }
                
                String containerId;
                try {
                    containerId = startContainer(project);
                    System.out.println("Container started with ID: " + containerId);
                } catch (Exception e) {
                    System.out.println("Error starting container: " + e.getMessage());
                    e.printStackTrace();
                    return ContainerInfo.builder()
                            .status(ContainerStatus.ERROR)
                            .build();
                }
                
                String hostPort;
                try {
                    hostPort = getContainerPort(project);
                    System.out.println("Container port: " + hostPort);
                } catch (Exception e) {
                    System.out.println("Error getting container port: " + e.getMessage());
                    hostPort = "3000";
                }
                
                var serverUrl = buildServerUrl(hostPort, project);
                System.out.println("Server URL: " + serverUrl);

                return ContainerInfo.builder()
                        .status(ContainerStatus.RUNNING)
                        .serverUrl(serverUrl)
                        .build();
            } catch (Exception e) {
                System.out.println("Unexpected error in startProject: " + e.getMessage());
                e.printStackTrace();
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
                var serverUrl = buildServerUrl(hostPort, project);

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
        String[] values = metrics.split(";");
        return new String[]{
                values[0].replace("%", ""),
                values[1].replace("%", "")
        };
    }

    public String getProjectUrl(String projectId) {
        try {
            var project = projectService.getProjectById(projectId);
            var hostPort = getContainerPort(project);
            return buildServerUrl(hostPort, project);
        } catch (Exception e) {
            return null;
        }
    }

    public void configureAutoRestart(String projectId, boolean enabled) {
        try {
            var project = projectService.getProjectById(projectId);
            var containerName = getContainerName(project);
            var containerStatus = getProjectStatus(projectId);
            
            log.info("Configuring auto-restart for project {} to {}", projectId, enabled);
            projectService.updateAutoRestartSetting(projectId, enabled);
            
            if (containerStatus == ContainerStatus.RUNNING) {
                String restartPolicy = enabled ? "unless-stopped" : "on-failure:3";
                log.info("Updating running container {} with restart policy: {}", containerName, restartPolicy);
                
                String[] command = new String[]{
                        "docker", "update", "--restart", restartPolicy, containerName
                };
                
                var processBuilder = new ProcessBuilder(command).redirectErrorStream(true);
                String output = executeDockerCommand(processBuilder);
                log.info("Docker update command output: {}", output);
                
                String[] verifyCommand = new String[]{
                        "docker", "inspect",
                        "--format", "{{.HostConfig.RestartPolicy.Name}}",
                        containerName
                };
                var verifyBuilder = new ProcessBuilder(verifyCommand).redirectErrorStream(true);
                String policy = executeDockerCommand(verifyBuilder);
                log.info("Verified restart policy for container {}: {}", containerName, policy);
            } else {
                log.info("Container {} is not running, restart policy will be applied on next start", containerName);
            }
            
        } catch (Exception e) {
            log.error("Failed to configure auto-restart for project {}: {}", projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to configure auto-restart for project " + projectId, e);
        }
    }

    public boolean isAutoRestartEnabled(String projectId) {
        try {
            var project = projectService.getProjectById(projectId);
            
            var containerStatus = getProjectStatus(projectId);
            if (containerStatus != ContainerStatus.RUNNING) {
                return project.isAutoRestartEnabled();
            }
            
            var command = new String[]{
                    "docker", "inspect",
                    "--format", "{{.HostConfig.RestartPolicy.Name}}",
                    getContainerName(project)
            };

            var processBuilder = new ProcessBuilder(command).redirectErrorStream(true);
            String policy = executeDockerCommand(processBuilder);

            return "always".equals(policy) || "unless-stopped".equals(policy);
        } catch (Exception e) {
            return false;
        }
    }
}