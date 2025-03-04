package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.repository.FileRepository;
import fr.umontpellier.bloomcycle.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FileService {

    private final FileRepository fileRepository;
    private final ProjectRepository projectRepository;

    @Value("${app.storage.path:/tmp/bloomcycle}")
    private String baseStoragePath;

    @Autowired
    public FileService(FileRepository fileRepository, ProjectRepository projectRepository) {
        this.fileRepository = fileRepository;
        this.projectRepository = projectRepository;
    }

    public String getProjectStoragePath(Long projectId) {
        return Paths.get(baseStoragePath, "projects", projectId.toString()).toString();
    }

    public void extractZipFile(MultipartFile zipFile, Path targetPath) throws IOException {
        String commonPrefix = findCommonPrefix(zipFile);
        extractFiles(zipFile, targetPath, commonPrefix);
    }

    private String findCommonPrefix(MultipartFile zipFile) throws IOException {
        try (var zipInputStream = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry firstEntry = zipInputStream.getNextEntry();
            if (firstEntry == null) {
                return "";
            }

            String potentialPrefix = firstEntry.isDirectory() ? firstEntry.getName() : "";

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.getName().startsWith(potentialPrefix)) {
                    return "";
                }
                zipInputStream.closeEntry();
            }

            return potentialPrefix;
        }
    }

    private void extractFiles(MultipartFile zipFile, Path targetPath, String commonPrefix) throws IOException {
        try (var zipInputStream = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                extractSingleEntry(entry, zipInputStream, targetPath, commonPrefix);
                zipInputStream.closeEntry();
            }
        }
    }

    private void extractSingleEntry(ZipEntry entry, ZipInputStream zipStream, Path targetPath, String commonPrefix) throws IOException {
        String entryName = getEntryName(entry, commonPrefix);
        if (entryName.isEmpty()) {
            return;
        }

        Path entryPath = targetPath.resolve(entryName);
        validatePath(entryPath, targetPath);

        if (entry.isDirectory()) {
            Files.createDirectories(entryPath);
        } else {
            extractFile(zipStream, entryPath);
        }
    }

    private String getEntryName(ZipEntry entry, String commonPrefix) {
        String entryName = entry.getName();
        if (!commonPrefix.isEmpty() && entryName.startsWith(commonPrefix)) {
            entryName = entryName.substring(commonPrefix.length());
        }
        return entryName;
    }

    private void validatePath(Path entryPath, Path targetPath) {
        if (!entryPath.normalize().startsWith(targetPath.normalize())) {
            throw new SecurityException("ZIP contains malicious paths");
        }
    }

    private void extractFile(ZipInputStream zipStream, Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent());
        Files.copy(zipStream, filePath, StandardCopyOption.REPLACE_EXISTING);
    }
}