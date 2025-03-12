package fr.umontpellier.bloomcycle.model.container;

import lombok.Getter;

@Getter
public enum ContainerOperation {
    START("up -d --build", "start"),
    STOP("down", "stop"),
    RESTART("restart", "restart");

    private final String command;
    private final String operationName;

    ContainerOperation(String command, String operationName) {
        this.command = command;
        this.operationName = operationName;
    }
} 