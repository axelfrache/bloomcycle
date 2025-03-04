package fr.umontpellier.bloomcycle.dto;

import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectResponse {
    private Long id;
    private String name;
    private String status;
    private User owner;

    public static ProjectResponse fromProject(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setStatus(project.getStatus());
        response.setOwner(project.getOwner());
        return response;
    }
}