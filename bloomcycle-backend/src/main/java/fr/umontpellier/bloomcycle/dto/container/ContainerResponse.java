package fr.umontpellier.bloomcycle.dto.container;

import fr.umontpellier.bloomcycle.model.container.ContainerStatus;
import fr.umontpellier.bloomcycle.model.container.ContainerInfo;
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
    private ContainerStatus status;
    private String serverUrl;

    public static ContainerResponse success(String operation) {
        return ContainerResponse.builder()
                .operation(operation)
                .status(ContainerStatus.RUNNING)
                .build();
    }

    public static ContainerResponse error(String operation, String errorMessage) {
        return ContainerResponse.builder()
                .operation(operation)
                .status(ContainerStatus.ERROR)
                .build();
    }

    public static ContainerResponse pending(String operation) {
        return ContainerResponse.builder()
                .operation(operation)
                .status(ContainerStatus.PENDING)
                .build();
    }

    public static ContainerResponse fromContainerInfo(ContainerInfo info, String operation) {
        return ContainerResponse.builder()
                .operation(operation)
                .status(info.getStatus())
                .serverUrl(info.getServerUrl())
                .build();
    }

    public static ContainerResponse fromStatus(ContainerStatus status) {
        return ContainerResponse.builder()
                .operation("status")
                .status(status)
                .build();
    }
} 