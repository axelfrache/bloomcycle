package fr.umontpellier.bloomcycle.service;

import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class GitService {

    public void cloneRepository(String repositoryUrl, String targetDirectory) {
        try {
            var targetPath = Paths.get(targetDirectory);
            Files.createDirectories(targetPath);

            Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(targetPath.toFile())
                .call()
                .close();
        } catch (Exception e) {
            throw new RuntimeException("Error cloning Git repository (" + repositoryUrl + "): " + e.getMessage(), e);
        }
    }
}