package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.model.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @InjectMocks
    private FileService fileService;

    @TempDir
    Path tempDir;

    private Project testProject;
    private Path storagePath;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setId("project123");
        storagePath = tempDir.resolve("storage");
        ReflectionTestUtils.setField(fileService, "storagePath", storagePath.toString());
    }

    @Test
    void getProjectStoragePath_ReturnsCorrectPath() {
        String result = fileService.getProjectStoragePath(testProject);
        assertEquals(
            storagePath.resolve("projects").resolve("project123").toString(),
            result
        );
    }

    public void extractZipFile(MultipartFile zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();

                // Protection contre les attaques zip-slip
                if (!entryPath.startsWith(targetDir)) {
                    throw new IOException("Entry is outside the target dir: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent()); // CRÉER LES DOSSIERS PARENTS
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }

                zis.closeEntry();
            }
        }
    }


    @Test
    void extractZipFile_ValidZip_Success() throws IOException {
        // Créer un fichier ZIP de test
        Path zipPath = tempDir.resolve("test.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            // Fichier racine
            ZipEntry entry = new ZipEntry("test.txt");
            zos.putNextEntry(entry);
            zos.write("Hello, World!".getBytes());
            zos.closeEntry();

            // Dossier vide
            entry = new ZipEntry("testdir/");
            zos.putNextEntry(entry);
            zos.closeEntry();

            // Fichier dans le dossier
            entry = new ZipEntry("testdir/nested.txt");
            zos.putNextEntry(entry);
            zos.write("Nested file".getBytes());
            zos.closeEntry();
        }

        // MockMultipartFile à partir du fichier ZIP
        byte[] zipContent = Files.readAllBytes(zipPath);
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.zip",
                "application/zip",
                zipContent
        );

        // Dossier cible
        Path targetPath = tempDir.resolve("extracted");

        // Appel à la méthode à tester
        fileService.extractZipFile(mockFile, targetPath);

        // Vérifications
        assertTrue(Files.exists(targetPath.resolve("test.txt")));
        assertTrue(Files.isDirectory(targetPath.resolve("testdir")));
        assertTrue(Files.exists(targetPath.resolve("testdir/nested.txt")));

        assertEquals(
                "Hello, World!",
                Files.readString(targetPath.resolve("test.txt"))
        );
        assertEquals(
                "Nested file",
                Files.readString(targetPath.resolve("testdir/nested.txt"))
        );
    }


    @Test
    void extractZipFile_ZipSlipAttempt_ThrowsException() throws IOException {
        // Créer un fichier ZIP malveillant avec un chemin relatif sortant du dossier cible
        Path zipPath = tempDir.resolve("malicious.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            ZipEntry entry = new ZipEntry("../../../etc/passwd");
            zos.putNextEntry(entry);
            zos.write("Malicious content".getBytes());
            zos.closeEntry();
        }

        byte[] zipContent = Files.readAllBytes(zipPath);
        MockMultipartFile mockFile = new MockMultipartFile(
            "file",
            "malicious.zip",
            "application/zip",
            zipContent
        );

        Path targetPath = tempDir.resolve("extracted");
        assertThrows(SecurityException.class, () ->
            fileService.extractZipFile(mockFile, targetPath)
        );
    }

    @Test
    void extractZipFile_TooLargeZip_ThrowsException() {
        // Créer un MockMultipartFile simulant un fichier trop grand
        byte[] largeContent = new byte[101 * 1024 * 1024]; // 101 MB
        MockMultipartFile mockFile = new MockMultipartFile(
            "file",
            "large.zip",
            "application/zip",
            largeContent
        );

        Path targetPath = tempDir.resolve("extracted");
        assertThrows(SecurityException.class, () ->
            fileService.extractZipFile(mockFile, targetPath)
        );
    }

    @Test
    void deleteProjectDirectory_Success() throws IOException {
        // Créer une structure de dossiers et fichiers de test
        Path projectPath = tempDir.resolve("project-to-delete");
        Files.createDirectories(projectPath.resolve("subdir"));
        Files.write(projectPath.resolve("test.txt"), "test".getBytes());
        Files.write(projectPath.resolve("subdir/nested.txt"), "nested".getBytes());

        // Vérifier que les fichiers existent
        assertTrue(Files.exists(projectPath));
        assertTrue(Files.exists(projectPath.resolve("test.txt")));
        assertTrue(Files.exists(projectPath.resolve("subdir/nested.txt")));

        // Supprimer le dossier
        fileService.deleteProjectDirectory(projectPath.toString());

        // Vérifier que tout a été supprimé
        assertFalse(Files.exists(projectPath));
    }

    @Test
    void deleteProjectDirectory_NonExistentDirectory_NoException() throws IOException {
        Path nonExistentPath = tempDir.resolve("non-existent");
        assertDoesNotThrow(() ->
            fileService.deleteProjectDirectory(nonExistentPath.toString())
        );
    }
}
