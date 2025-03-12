package fr.umontpellier.bloomcycle.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Slf4j
public class ProjectTypeAnalyzer {

    public enum TechnologyStack {
        JAVA_MAVEN,
        NODEJS,
        PYTHON,
        UNKNOWN
    }

    public TechnologyStack analyzeTechnology(String projectPath) {
        log.info("Analyzing project technology at path: {}", projectPath);
        
        var dockerfile = Path.of(projectPath, "Dockerfile");
        if (Files.exists(dockerfile)) {
            try {
                var dockerfileContent = Files.readString(dockerfile);
                var detectedTech = analyzeDockerfile(dockerfileContent);
                if (detectedTech != TechnologyStack.UNKNOWN) {
                    log.info("Technology detected from Dockerfile: {}", detectedTech);
                    return detectedTech;
                }
            } catch (IOException e) {
                log.warn("Could not read Dockerfile: {}", e.getMessage());
            }
        }

        return analyzeProjectFiles(projectPath);
    }

    private TechnologyStack analyzeDockerfile(String content) {
        content = content.toLowerCase();
        
        if (content.contains("from openjdk") || 
            content.contains("from maven") || 
            content.contains("from gradle")) {
            return TechnologyStack.JAVA_MAVEN;
        }
        
        if (content.contains("from node") || 
            content.contains("npm install") || 
            content.contains("yarn install")) {
            return TechnologyStack.NODEJS;
        }
        
        if (content.contains("from python") || 
            content.contains("pip install") || 
            content.contains("requirements.txt")) {
            return TechnologyStack.PYTHON;
        }

        if (content.contains("mvn") || content.contains(".jar")) {
            return TechnologyStack.JAVA_MAVEN;
        }
        
        if (content.contains("package.json") || 
            content.contains("node_modules") || 
            content.contains("npm") || 
            content.contains("yarn")) {
            return TechnologyStack.NODEJS;
        }
        
        if (content.contains("python") || 
            content.contains("pip") || 
            content.contains(".py")) {
            return TechnologyStack.PYTHON;
        }

        return TechnologyStack.UNKNOWN;
    }

    private TechnologyStack analyzeProjectFiles(String projectPath) {
        try (Stream<Path> paths = Files.walk(Path.of(projectPath))) {
            var files = paths
                .filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map(Path::toString)
                .toList();

            if (hasJavaFiles(files)) {
                return TechnologyStack.JAVA_MAVEN;
            }

            if (hasNodeFiles(files)) {
                return TechnologyStack.NODEJS;
            }

            if (hasPythonFiles(files)) {
                return TechnologyStack.PYTHON;
            }

            return TechnologyStack.UNKNOWN;
        } catch (IOException e) {
            log.error("Error analyzing project files: {}", e.getMessage());
            return TechnologyStack.UNKNOWN;
        }
    }

    private boolean hasJavaFiles(List<String> files) {
        return files.stream().anyMatch(f -> 
            f.equals("pom.xml") || 
            f.equals("build.gradle") || 
            f.endsWith(".java")
        );
    }

    private boolean hasNodeFiles(List<String> files) {
        return files.stream().anyMatch(f -> 
            f.equals("package.json") || 
            f.equals("package-lock.json") || 
            f.equals("yarn.lock") || 
            f.endsWith(".js") || 
            f.endsWith(".ts")
        );
    }

    private boolean hasPythonFiles(List<String> files) {
        return files.stream().anyMatch(f -> 
            f.equals("requirements.txt") || 
            f.equals("setup.py") || 
            f.endsWith(".py")
        );
    }

    public Optional<String> findContainerization(String projectPath) {
        var basePath = Path.of(projectPath);

        return Stream.of(
                        "docker-compose.yml",
                        "docker-compose.yaml"
                )
                .map(basePath::resolve)
                .filter(Files::exists)
                .findFirst()
                .map(Path::toString);
    }
} 