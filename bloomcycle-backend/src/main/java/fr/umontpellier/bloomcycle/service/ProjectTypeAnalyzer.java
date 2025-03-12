package fr.umontpellier.bloomcycle.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ProjectTypeAnalyzer {

    @Getter
    public enum TechnologyStack {
        JAVA_MAVEN(List.of("pom.xml")),
        JAVA_GRADLE(List.of("build.gradle", "build.gradle.kts")),
        NODEJS(List.of("package.json")),
        PYTHON(List.of("requirements.txt", "setup.py", "pyproject.toml")),
        UNKNOWN(List.of());

        private final List<String> markers;

        TechnologyStack(List<String> markers) {
            this.markers = markers;
        }

    }

    public TechnologyStack analyzeTechnology(String projectPath) {
        var basePath = Path.of(projectPath);

        return Stream.of(TechnologyStack.values())
                .filter(tech -> tech != TechnologyStack.UNKNOWN)
                .filter(tech -> hasAnyMarker(basePath, tech.getMarkers()))
                .findFirst()
                .orElse(TechnologyStack.UNKNOWN);
    }

    private boolean hasAnyMarker(Path basePath, List<String> markers) {
        return markers.stream()
                .anyMatch(marker -> Files.exists(basePath.resolve(marker)));
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