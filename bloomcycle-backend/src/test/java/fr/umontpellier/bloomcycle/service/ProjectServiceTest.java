package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.exception.ResourceNotFoundException;
import fr.umontpellier.bloomcycle.exception.UnauthorizedAccessException;
import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.model.User;
import fr.umontpellier.bloomcycle.repository.ProjectRepository;
import fr.umontpellier.bloomcycle.service.ProjectTypeAnalyzer.TechnologyStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private FileService fileService;

    @Mock
    private GitService gitService;

    @Mock
    private ProjectTypeAnalyzer projectAnalyzer;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private ProjectService projectService;

    private User testUser;
    private Project testProject;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testProject = new Project();
        testProject.setId("project123");
        testProject.setName("Test Project");
        testProject.setOwner(testUser);

        authentication = new UsernamePasswordAuthenticationToken(testUser, null);
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Test
    void getProjectById_ExistingProject_ReturnsProject() {
        when(projectRepository.findById("project123")).thenReturn(Optional.of(testProject));

        Project result = projectService.getProjectById("project123");

        assertNotNull(result);
        assertEquals("project123", result.getId());
        assertEquals("Test Project", result.getName());
        verify(projectRepository).findById("project123");
    }

    @Test
    void getProjectById_NonExistingProject_ThrowsException() {
        when(projectRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            projectService.getProjectById("nonexistent")
        );
        verify(projectRepository).findById("nonexistent");
    }

    @Test
    void getProjectsByUser_ReturnsUserProjects() {
        List<Project> projects = Arrays.asList(testProject);
        when(projectRepository.findByOwner(testUser)).thenReturn(projects);

        List<Project> result = projectService.getProjectsByUser(testUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("project123", result.get(0).getId());
        verify(projectRepository).findByOwner(testUser);
    }

    @Test
    void getCurrentUserProjects_ReturnsCurrentUserProjects() {
        List<Project> projects = Arrays.asList(testProject);
        when(projectRepository.findByOwner(testUser)).thenReturn(projects);

        List<Project> result = projectService.getCurrentUserProjects();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("project123", result.get(0).getId());
        verify(projectRepository).findByOwner(testUser);
    }

    @Test
    void initializeProjectFromGit_Success() throws IOException {
        String projectName = "Git Project";
        String gitUrl = "https://github.com/test/repo.git";
        Path tempDir = Files.createTempDirectory("test-project-git");
        String projectPath = tempDir.toString();

        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(fileService.getProjectStoragePath(any(Project.class))).thenReturn(projectPath);
        when(projectAnalyzer.analyzeTechnology(projectPath)).thenReturn(TechnologyStack.JAVA_MAVEN);

        Project result = projectService.initializeProjectFromGit(projectName, gitUrl);

        assertNotNull(result);
        assertEquals(testProject.getId(), result.getId());
        verify(gitService).cloneRepository(gitUrl, projectPath);
        verify(projectAnalyzer).analyzeTechnology(projectPath);
    }


    @Test
    void initializeProjectFromZip_Success() throws IOException {
        String projectName = "Zip Project";
        Path tempDir = Files.createTempDirectory("test-project");
        String projectPath = tempDir.toString();
        MockMultipartFile zipFile = new MockMultipartFile(
                "file", "test.zip", "application/zip", "test data".getBytes()
        );

        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(fileService.getProjectStoragePath(any(Project.class))).thenReturn(projectPath);
        when(projectAnalyzer.analyzeTechnology(projectPath)).thenReturn(TechnologyStack.NODEJS);

        Project result = projectService.initializeProjectFromZip(projectName, zipFile);

        assertNotNull(result);
        assertEquals(testProject.getId(), result.getId());
        verify(fileService).extractZipFile(eq(zipFile), any(Path.class));
        verify(projectAnalyzer).analyzeTechnology(projectPath);
    }

    @Test
    void deleteProject_Success() throws IOException {
        when(projectRepository.findById("project123")).thenReturn(Optional.of(testProject));
        when(fileService.getProjectStoragePath(testProject)).thenReturn("/path/to/project");

        projectService.deleteProject("project123");

        verify(projectRepository).delete(testProject);
        verify(fileService).deleteProjectDirectory("/path/to/project");
    }

    @Test
    void deleteProject_UnauthorizedUser_ThrowsException() {
        User otherUser = new User();
        otherUser.setId(2L);
        Project otherProject = new Project();
        otherProject.setId("project123");
        otherProject.setOwner(otherUser);

        when(projectRepository.findById("project123")).thenReturn(Optional.of(otherProject));

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                projectService.deleteProject("project123")
        );
        assertTrue(thrown.getCause() instanceof UnauthorizedAccessException);
        verify(projectRepository, never()).delete(any());
    }


    @Test
    void getProjectTechnology_Success() {
        when(projectRepository.findById("project123")).thenReturn(Optional.of(testProject));
        when(fileService.getProjectStoragePath(testProject)).thenReturn("/path/to/project");
        when(projectAnalyzer.analyzeTechnology("/path/to/project")).thenReturn(TechnologyStack.JAVA_MAVEN);

        String result = projectService.getProjectTechnology("project123");

        assertEquals("JAVA_MAVEN", result);
        verify(projectAnalyzer).analyzeTechnology("/path/to/project");
    }
}
