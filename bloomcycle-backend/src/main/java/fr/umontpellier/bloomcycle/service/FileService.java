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
        var commonPrefix = findCommonPrefix(zipFile);
        extractEntriesFromZip(zipFile, targetPath, commonPrefix);
    }

    private String findCommonPrefix(MultipartFile zipFile) throws IOException {
        try (var zip = new ZipInputStream(zipFile.getInputStream())) {
            var firstEntry = zip.getNextEntry();
            if (firstEntry == null) return "";

            var prefix = firstEntry.isDirectory() ? firstEntry.getName() : "";
            return isCommonPrefixValid(zip, prefix) ? prefix : "";
        }
    }

    private boolean isCommonPrefixValid(ZipInputStream zip, String prefix) throws IOException {
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            if (!entry.getName().startsWith(prefix)) return false;
            zip.closeEntry();
        }
        return true;
    }

    private void extractEntriesFromZip(MultipartFile zipFile, Path targetPath, String commonPrefix) throws IOException {
        try (var zip = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                processZipEntry(entry, zip, targetPath, commonPrefix);
                zip.closeEntry();
            }
        }
    }

    private void processZipEntry(ZipEntry entry, ZipInputStream zip, Path targetPath, String commonPrefix) throws IOException {
        var entryName = getRelativeEntryName(entry.getName(), commonPrefix);
        if (entryName.isEmpty()) return;

        var entryPath = targetPath.resolve(entryName);
        ensurePathSecurity(entryPath, targetPath);

        if (entry.isDirectory()) {
            Files.createDirectories(entryPath);
        } else {
            extractFileEntry(zip, entryPath);
        }
    }

    private String getRelativeEntryName(String entryName, String commonPrefix) {
        return commonPrefix.isEmpty() ? entryName : 
               entryName.startsWith(commonPrefix) ? entryName.substring(commonPrefix.length()) : 
               entryName;
    }

    private void ensurePathSecurity(Path entryPath, Path targetPath) {
        if (!entryPath.normalize().startsWith(targetPath.normalize())) {
            throw new SecurityException("ZIP contains malicious paths");
        }
    }

    private void extractFileEntry(ZipInputStream zip, Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent());
        Files.copy(zip, filePath, StandardCopyOption.REPLACE_EXISTING);
    }
}