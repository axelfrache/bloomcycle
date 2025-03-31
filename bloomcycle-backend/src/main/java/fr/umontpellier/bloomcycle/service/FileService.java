package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.model.Project;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class FileService {



    @Value("${app.storage.path}")
    private String storagePath;

    public String getProjectStoragePath(Project project) {
        return Path.of(storagePath, "projects", project.getId()).toString();
    }

    private static final long MAX_ZIP_SIZE = 100 * 1024 * 1024; // 100 MB
    private static final int MAX_FILES = 1000;
    private static final int MAX_PATH_LENGTH = 255;

    public void extractZipFile(MultipartFile file, Path targetPath) throws IOException {
        if (file.getSize() > MAX_ZIP_SIZE)
            throw new SecurityException("ZIP file too large: " + file.getSize() + " bytes (max: " + MAX_ZIP_SIZE + " bytes)");

        Files.createDirectories(targetPath);

        try (var zipInputStream = new ZipInputStream(file.getInputStream())) {
            var entry = zipInputStream.getNextEntry();
            int fileCount = 0;
            String commonPrefix = null;

            while (entry != null) {
                var name = entry.getName().replace("\\", "/");
                if (commonPrefix == null) {
                    int firstSlash = name.indexOf('/');
                    if (firstSlash != -1) {
                        commonPrefix = name.substring(0, firstSlash + 1);
                    }
                } else if (!name.startsWith(commonPrefix)) {
                    commonPrefix = null;
                    break;
                }
                entry = zipInputStream.getNextEntry();
            }

            zipInputStream.close();
            try (var newZipStream = new ZipInputStream(file.getInputStream())) {
                entry = newZipStream.getNextEntry();
                while (entry != null && fileCount < MAX_FILES) {
                    String name = entry.getName().replace("\\", "/");
                    
                    if (commonPrefix != null && name.startsWith(commonPrefix))
                        name = name.substring(commonPrefix.length());

                    if (name.length() > MAX_PATH_LENGTH)
                        throw new SecurityException("Path too long: " + name);

                    name = name.replaceAll("[^a-zA-Z0-9./\\-_]+", "_");
                    var entryPath = targetPath.resolve(name).normalize();

                    if (!entryPath.startsWith(targetPath.normalize()))
                        throw new SecurityException("ZIP entry contains invalid path: " + name);

                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        Files.copy(newZipStream, entryPath, StandardCopyOption.REPLACE_EXISTING);
                        fileCount++;
                    }

                    entry = newZipStream.getNextEntry();
                }

                if (fileCount >= MAX_FILES)
                    throw new SecurityException("Too many files in ZIP (max: " + MAX_FILES + ")");
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