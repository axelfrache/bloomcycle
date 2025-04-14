package fr.umontpellier.bloomcycle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.umontpellier.bloomcycle.dto.AutoRestartRequest;
import fr.umontpellier.bloomcycle.dto.ProjectResponse;
import fr.umontpellier.bloomcycle.dto.container.ContainerResponse;
import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.model.User;
import fr.umontpellier.bloomcycle.model.container.ContainerInfo;
import fr.umontpellier.bloomcycle.model.container.ContainerOperation;
import fr.umontpellier.bloomcycle.model.container.ContainerStatus;
import fr.umontpellier.bloomcycle.security.JwtService;
import fr.umontpellier.bloomcycle.service.DockerService;
import fr.umontpellier.bloomcycle.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false) // facultatif, pour désactiver les filtres (ex : sécurité)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private DockerService dockerService;

     @MockBean
     private JwtService jwtService;

    private User currentUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .email("user@test.com")
                .username("testuser")
                .build();

        testProject = Project.builder()
                .id("project1")
                .name("Test Project")
                .owner(currentUser)
                .build();

        // Mock security context
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(currentUser);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createProject_ShouldReturnCreated_WhenValidGitUrl() throws Exception {
        // Arrange
        String name = "Test Project";
        String gitUrl = "https://github.com/test/repo.git";
        String token = jwtService.generateToken(currentUser);
        when(projectService.initializeProjectFromGit(eq(name), eq(gitUrl))).thenReturn(testProject);
        when(dockerService.getProjectStatus(testProject.getId())).thenReturn(ContainerStatus.STOPPED);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/projects")
                        .param("name", name)
                        .param("gitUrl", gitUrl)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testProject.getId()))
                .andExpect(jsonPath("$.name").value(testProject.getName()))
                .andExpect(jsonPath("$.status").value(ContainerStatus.STOPPED.toString()));
    }

    @Test
    void createProject_ShouldReturnCreated_WhenValidZipFile() throws Exception {
        // Arrange
        String name = "Test Project";
        MockMultipartFile sourceZip = new MockMultipartFile(
                "sourceZip",
                "test.zip",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "test content".getBytes()
        );

        when(projectService.initializeProjectFromZip(eq(name), any())).thenReturn(testProject);
        when(dockerService.getProjectStatus(testProject.getId())).thenReturn(ContainerStatus.STOPPED);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/projects")
                        .file(sourceZip)
                        .param("name", name))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testProject.getId()))
                .andExpect(jsonPath("$.name").value(testProject.getName()))
                .andExpect(jsonPath("$.status").value(ContainerStatus.STOPPED.toString()));
    }

    @Test
    void getAllProjects_ShouldReturnProjects_WhenUserHasProjects() throws Exception {
        // Arrange
        Project project2 = Project.builder()
                .id("project2")
                .name("Test Project 2")
                .owner(currentUser)
                .build();

        List<Project> projects = Arrays.asList(testProject, project2);
        when(projectService.getCurrentUserProjects()).thenReturn(projects);
        when(dockerService.getProjectStatus(any())).thenReturn(ContainerStatus.RUNNING);

        // Act & Assert
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testProject.getId()))
                .andExpect(jsonPath("$[0].name").value(testProject.getName()))
                .andExpect(jsonPath("$[0].status").value(ContainerStatus.RUNNING.toString()))
                .andExpect(jsonPath("$[1].id").value(project2.getId()))
                .andExpect(jsonPath("$[1].name").value(project2.getName()))
                .andExpect(jsonPath("$[1].status").value(ContainerStatus.RUNNING.toString()));
    }

    @Test
    void startProject_ShouldReturnOk_WhenProjectExists() throws Exception {
        // Arrange
        ContainerInfo containerInfo = ContainerInfo.builder()
                .status(ContainerStatus.RUNNING)
                .build();

        when(projectService.getProjectById(testProject.getId())).thenReturn(testProject);
        when(dockerService.executeOperation(testProject.getId(), ContainerOperation.START))
                .thenReturn(CompletableFuture.completedFuture(containerInfo));

        // Act & Assert
        mockMvc.perform(post("/api/v1/projects/{id}/start", testProject.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ContainerStatus.RUNNING.toString()))
                .andExpect(jsonPath("$.operation").value("start"))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void stopProject_ShouldReturnAccepted_WhenProjectExists() throws Exception {
        // Arrange
        ContainerInfo containerInfo = ContainerInfo.builder()
                .status(ContainerStatus.STOPPED)
                .build();

        when(projectService.getProjectById(testProject.getId())).thenReturn(testProject);
        when(dockerService.executeOperation(testProject.getId(), ContainerOperation.STOP))
                .thenReturn(CompletableFuture.completedFuture(containerInfo));

        // Act & Assert
        mockMvc.perform(post("/api/v1/projects/{id}/stop", testProject.getId()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value(ContainerStatus.STOPPED.toString()))
                .andExpect(jsonPath("$.operation").value("stop"))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void configureAutoRestart_ShouldReturnOk_WhenValidRequest() throws Exception {
        // Arrange
        AutoRestartRequest request = AutoRestartRequest.builder()
                .enabled(true)
                .build();

        when(projectService.getProjectById(testProject.getId())).thenReturn(testProject);

        // Act & Assert
        mockMvc.perform(post("/api/v1/projects/{id}/auto-restart", testProject.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.projectId").value(testProject.getId()))
                .andExpect(jsonPath("$.autoRestartEnabled").value(true));
    }
}
