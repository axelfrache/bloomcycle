package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.repository.FileRepository;
import fr.umontpellier.bloomcycle.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final ProjectRepository projectRepository;

    @Value("${app.storage.path}")
    private String storagePath;

    public String getProjectStoragePath(Project project) {
        return Path.of(storagePath, "projects", project.getId()).toString();
    }

    public void extractZipFile(MultipartFile file, Path targetPath) throws IOException {
        try (var zipInputStream = new ZipInputStream(file.getInputStream())) {
            var entry = zipInputStream.getNextEntry();
            while (entry != null) {
                var entryPath = targetPath.resolve(entry.getName());

                if (!entryPath.normalize().startsWith(targetPath.normalize()))
                    throw new SecurityException("ZIP entry contains invalid path: " + entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zipInputStream, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                entry = zipInputStream.getNextEntry();
            }
        }
    }

    public void deleteProjectDirectory(String projectPath) throws IOException {
        var path = Path.of(projectPath);
        if (Files.exists(path)) {
            try (var pathStream = Files.walk(path)) {
                var success = pathStream
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .allMatch(File::delete);
                if (!success) {
                    throw new IOException("Failed to delete some files in directory: " + projectPath);
                }
            }
        }
    }
}