package fr.umontpellier.bloomcycle.model.container;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerInfo {
    private ContainerStatus status;
    private String serverUrl;
} 