package fr.umontpellier.bloomcycle.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitServiceTest {

    private GitService gitService;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        gitService = new GitService();
    }

    @Test
    void cloneRepository_ShouldCloneRepo_WhenValidUrl() {
        // Arrange
        String repoUrl = "https://github.com/octocat/Hello-World.git";
        String targetDirectory = tempDir.resolve("test-repo").toString();

        // Act
        gitService.cloneRepository(repoUrl, targetDirectory);

        // Assert
        Path gitDir = Path.of(targetDirectory, ".git");
        assertThat(Files.exists(gitDir)).isTrue();
        assertThat(Files.isDirectory(gitDir)).isTrue();
    }

    @Test
    void cloneRepository_ShouldThrowException_WhenInvalidUrl() {
        // Arrange
        String invalidRepoUrl = "https://invalid-url.git";
        String targetDirectory = tempDir.resolve("invalid-repo").toString();

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> gitService.cloneRepository(invalidRepoUrl, targetDirectory));
    }

    @Test
    void cloneRepository_ShouldCreateDirectory_WhenDirectoryDoesNotExist() {
        // Arrange
        String repoUrl = "https://github.com/octocat/Hello-World.git";
        String targetDirectory = tempDir.resolve("nested/test-repo").toString();

        // Act
        gitService.cloneRepository(repoUrl, targetDirectory);

        // Assert
        assertThat(Files.exists(Path.of(targetDirectory))).isTrue();
        assertThat(Files.isDirectory(Path.of(targetDirectory))).isTrue();
    }
}
