package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.model.container.ContainerOperation;
import fr.umontpellier.bloomcycle.model.container.ContainerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContainerMonitorServiceTest {

    @Mock
    private DockerService dockerService;

    @Mock
    private ProjectService projectService;

    private ContainerMonitorService containerMonitorService;

    @BeforeEach
    void setUp() {
        containerMonitorService = new ContainerMonitorService(dockerService, projectService);
    }

    @Test
    void checkContainers_ShouldRestartStoppedContainers_WhenAutoRestartEnabled() {
        // Arrange
        Project project1 = new Project();
        project1.setId("1");
        project1.setAutoRestartEnabled(true);

        Project project2 = new Project();
        project2.setId("2");
        project2.setAutoRestartEnabled(true);

        when(projectService.getAllProjects()).thenReturn(Arrays.asList(project1, project2));
        when(dockerService.getProjectStatus("1")).thenReturn(ContainerStatus.STOPPED);
        when(dockerService.getProjectStatus("2")).thenReturn(ContainerStatus.RUNNING);

        // Act
        // We need to use reflection to access the private method
        ReflectionTestUtils.invokeMethod(containerMonitorService, "checkContainers");

        // Assert
        verify(dockerService).executeOperation("1", ContainerOperation.START);
        verify(dockerService, never()).executeOperation("2", ContainerOperation.START);
    }

    @Test
    void checkContainers_ShouldNotRestartContainers_WhenAutoRestartDisabled() {
        // Arrange
        Project project = new Project();
        project.setId("1");
        project.setAutoRestartEnabled(false);

        when(projectService.getAllProjects()).thenReturn(Collections.singletonList(project));

        // Act
        ReflectionTestUtils.invokeMethod(containerMonitorService, "checkContainers");

        // Assert
        verify(dockerService, never()).executeOperation(anyString(), any(ContainerOperation.class));
    }

    @Test
    void checkContainers_ShouldHandleExceptions_WhenDockerServiceFails() {
        // Arrange
        Project project = new Project();
        project.setId("1");
        project.setAutoRestartEnabled(true);

        when(projectService.getAllProjects()).thenReturn(Collections.singletonList(project));
        when(dockerService.getProjectStatus("1")).thenThrow(new RuntimeException("Docker service error"));

        // Act
        ReflectionTestUtils.invokeMethod(containerMonitorService, "checkContainers");

        // Assert
        verify(dockerService, never()).executeOperation(anyString(), any(ContainerOperation.class));
    }

    @Test
    void checkContainers_ShouldHandleMultipleProjects() {
        // Arrange
        Project project1 = new Project();
        project1.setId("1");
        project1.setAutoRestartEnabled(true);

        Project project2 = new Project();
        project2.setId("2");
        project2.setAutoRestartEnabled(true);

        Project project3 = new Project();
        project3.setId("3");
        project3.setAutoRestartEnabled(false);

        when(projectService.getAllProjects()).thenReturn(Arrays.asList(project1, project2, project3));
        when(dockerService.getProjectStatus("1")).thenReturn(ContainerStatus.STOPPED);
        when(dockerService.getProjectStatus("2")).thenReturn(ContainerStatus.STOPPED);

        // Act
        ReflectionTestUtils.invokeMethod(containerMonitorService, "checkContainers");

        // Assert
        verify(dockerService).executeOperation("1", ContainerOperation.START);
        verify(dockerService).executeOperation("2", ContainerOperation.START);
        verify(dockerService, never()).getProjectStatus("3");
        verify(dockerService, never()).executeOperation("3", ContainerOperation.START);
    }
}
