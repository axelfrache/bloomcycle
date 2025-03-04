package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.model.Project;
import fr.umontpellier.bloomcycle.repository.FileRepository;
import fr.umontpellier.bloomcycle.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Service
public class FileService {

    private final FileRepository fileRepository;
    private final ProjectRepository projectRepository;

    @Value("${app.file-storage.base-path}")
    private String baseStoragePath;

    @Autowired
    public FileService(FileRepository fileRepository, ProjectRepository projectRepository) {
        this.fileRepository = fileRepository;
        this.projectRepository = projectRepository;
    }

    public String getProjectStoragePath(Long projectId) {
        return baseStoragePath + "/" + projectId;
    }

    public fr.umontpellier.bloomcycle.model.File addFileToProject(Long projectId, fr.umontpellier.bloomcycle.model.File file) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        file.setProject(project);
        return fileRepository.save(file);
    }

    public void deleteFile(Long fileId) {
        var file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        try {
            Files.deleteIfExists(new File(file.getFilePath()).toPath());
        } catch (IOException e) {
            throw new RuntimeException("Error deleting file: " + file.getName(), e);
        }

        fileRepository.deleteById(fileId);
    }

    public fr.umontpellier.bloomcycle.model.File getFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    public void uploadSourcesToProject(Long projectId, String sourcePath) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        var sourceDirectory = new File(sourcePath);
        if (!sourceDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid source path: Not a directory");
        }

        String projectStoragePath = getProjectStoragePath(projectId);
        new File(projectStoragePath).mkdirs();

        for (var child : Objects.requireNonNull(sourceDirectory.listFiles())) {
            copyDirectoryRecursively(child, new File(projectStoragePath), project);
        }
    }

    private void copyDirectoryRecursively(File source, File target, Project project) {
        if (source.isDirectory()) {
            var newDir = new File(target, source.getName());
            newDir.mkdir();

            for (var child : Objects.requireNonNull(source.listFiles())) {
                copyDirectoryRecursively(child, newDir, project);
            }
        } else {
            try {
                var targetFile = new File(target, source.getName());
                Files.copy(source.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                fr.umontpellier.bloomcycle.model.File newFile = new fr.umontpellier.bloomcycle.model.File();
                newFile.setName(source.getName());
                newFile.setType(Files.probeContentType(source.toPath()));
                newFile.setFilePath(targetFile.getAbsolutePath());
                newFile.setProject(project);

                fileRepository.save(newFile);
            } catch (IOException e) {
                throw new RuntimeException("Error copying file: " + source.getName(), e);
            }
        }
    }

    public byte[] getFileContent(Long fileId) throws IOException {
        var file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        return Files.readAllBytes(new File(file.getFilePath()).toPath());
    }
}