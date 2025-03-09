package fr.umontpellier.bloomcycle.dto;

import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.model.User;
import fr.umontpellier.bloomcycle.model.container.ContainerStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private User owner;
    private ContainerStatus containerStatus;

    public static ProjectResponse fromProject(Project project, ContainerStatus containerStatus) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .owner(project.getOwner())
                .containerStatus(containerStatus)
                .build();
    }
}