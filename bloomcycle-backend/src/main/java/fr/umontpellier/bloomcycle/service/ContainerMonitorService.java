package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.model.container.ContainerOperation;
import fr.umontpellier.bloomcycle.model.container.ContainerStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ContainerMonitorService {
    private final DockerService dockerService;
    private final ProjectService projectService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @PostConstruct
    public void startMonitoring() {
        scheduler.scheduleAtFixedRate(this::checkContainers, 0, 1, TimeUnit.MINUTES);
    }
    
    private void checkContainers() {
        projectService.getAllProjects().stream()
            .filter(Project::isAutoRestartEnabled)
            .forEach(project -> {
                try {
                    var status = dockerService.getProjectStatus(project.getId());
                    if (status == ContainerStatus.STOPPED) {
                        dockerService.executeOperation(project.getId(), ContainerOperation.START);
                    }
                } catch (Exception e) {

                }
            });
    }
} 