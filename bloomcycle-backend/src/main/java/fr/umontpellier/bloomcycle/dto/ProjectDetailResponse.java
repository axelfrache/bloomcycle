package fr.umontpellier.bloomcycle.dto;

import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.model.container.ContainerStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectDetailResponse {
    private String id;
    private String name;
    private String owner;
    private ContainerStatus containerStatus;
    private String cpuUsage;
    private String memoryUsage;
    private String serverUrl;
    private String technology;
    private boolean autoRestartEnabled;

    public static ProjectDetailResponse fromProject(Project project, ContainerStatus status, String cpuUsage, String memoryUsage, String serverUrl, String technology, boolean autoRestartEnabled) {
        return ProjectDetailResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .owner(project.getOwner().getEmail())
                .containerStatus(status)
                .cpuUsage(cpuUsage)
                .memoryUsage(memoryUsage)
                .serverUrl(status == ContainerStatus.RUNNING ? serverUrl : null)
                .technology(technology)
                .autoRestartEnabled(autoRestartEnabled)
                .build();
    }
} 