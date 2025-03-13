package fr.umontpellier.bloomcycle.model.container;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

@JsonFormat(shape = JsonFormat.Shape.STRING)
@Getter
public enum ContainerStatus {
    RUNNING("Container is running"),
    STOPPED("Container is stopped"),
    ERROR("Container encountered an error"),
    PENDING("Container is pending");

    private final String description;

    ContainerStatus(String description) {
        this.description = description;
    }
} 