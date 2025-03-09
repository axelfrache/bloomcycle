package fr.umontpellier.bloomcycle.dto.container;

import fr.umontpellier.bloomcycle.model.container.ContainerStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerResponse {
    private String message;
    private String status;
    private ContainerStatus containerStatus;

    public static ContainerResponse fromStatus(ContainerStatus status) {
        return ContainerResponse.builder()
                .containerStatus(status)
                .status(status.name())
                .message(status.getDescription())
                .build();
    }

    public static ContainerResponse pending(String operation) {
        return ContainerResponse.builder()
                .status("PENDING")
                .message(String.format("Project %s initiated", operation))
                .build();
    }

    public static ContainerResponse error(String operation, String errorMessage) {
        return ContainerResponse.builder()
                .status("ERROR")
                .message(String.format("Failed to %s project: %s", operation, errorMessage))
                .build();
    }
} 