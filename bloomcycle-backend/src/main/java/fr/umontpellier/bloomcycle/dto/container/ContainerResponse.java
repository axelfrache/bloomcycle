package fr.umontpellier.bloomcycle.dto.container;

import fr.umontpellier.bloomcycle.model.container.ContainerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContainerResponse {
    private String operation;
    private String status;
    private String message;
    private ContainerStatus containerStatus;

    public static ContainerResponse success(String operation) {
        return ContainerResponse.builder()
                .operation(operation)
                .status("SUCCESS")
                .message("Operation completed successfully")
                .build();
    }

    public static ContainerResponse error(String operation, String errorMessage) {
        return ContainerResponse.builder()
                .operation(operation)
                .status("ERROR")
                .message(errorMessage)
                .build();
    }

    public static ContainerResponse pending(String operation) {
        return ContainerResponse.builder()
                .operation(operation)
                .status("PENDING")
                .message("Operation in progress")
                .build();
    }

    public static ContainerResponse fromStatus(ContainerStatus status) {
        return ContainerResponse.builder()
                .operation("status")
                .status(status.name())
                .message("Container status retrieved")
                .build();
    }
} 