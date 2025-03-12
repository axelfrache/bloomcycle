package fr.umontpellier.bloomcycle.dto;

import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.model.container.ContainerStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectDetailResponse {
    private Long id;
    private String name;
    private String owner;
    private ContainerStatus containerStatus;
    private String serverUrl;
    private String technology;

    public static ProjectDetailResponse fromProject(Project project, ContainerStatus status, String serverUrl, String technology) {
        return ProjectDetailResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .owner(project.getOwner().getEmail())
                .containerStatus(status)
                .serverUrl(status == ContainerStatus.RUNNING ? serverUrl : null)
                .technology(technology)
                .build();
    }
} 