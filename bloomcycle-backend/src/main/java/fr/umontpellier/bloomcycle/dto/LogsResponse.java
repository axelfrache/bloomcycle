package fr.umontpellier.bloomcycle.dto;

import fr.umontpellier.bloomcycle.model.Project;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogsResponse {
    String id;
    String name;
    String owner;
    String logs;

    public static LogsResponse fromLogs(Project project, String logs) {
        return LogsResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .owner(project.getOwner().getEmail())
                .logs(logs)
                .build();
    }
}