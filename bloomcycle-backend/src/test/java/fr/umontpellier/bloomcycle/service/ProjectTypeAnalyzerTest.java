package fr.umontpellier.bloomcycle.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProjectTypeAnalyzerTest {

    @InjectMocks
    private ProjectTypeAnalyzer projectTypeAnalyzer;

    @TempDir
    Path tempDir;

    @Test
    void analyzeTechnology_JavaMavenProject_ReturnsJavaMaven() throws IOException {
        // Créer un projet Maven
        Files.write(tempDir.resolve("pom.xml"),
            """
            &lt;project&gt;
                &lt;modelVersion&gt;4.0.0&lt;/modelVersion&gt;
                &lt;groupId&gt;com.example&lt;/groupId&gt;
                &lt;artifactId&gt;test-project&lt;/artifactId&gt;
                &lt;version&gt;1.0.0&lt;/version&gt;
            &lt;/project&gt;
            """.getBytes()
        );

        var result = projectTypeAnalyzer.analyzeTechnology(tempDir.toString());
        assertEquals(ProjectTypeAnalyzer.TechnologyStack.JAVA_MAVEN, result);
    }

    @Test
    void analyzeTechnology_NodejsProject_ReturnsNodejs() throws IOException {
        // Créer un projet Node.js
        Files.write(tempDir.resolve("package.json"),
            """
            {
                "name": "test-project",
                "version": "1.0.0",
                "dependencies": {
                    "express": "^4.17.1"
                }
            }
            """.getBytes()
        );

        var result = projectTypeAnalyzer.analyzeTechnology(tempDir.toString());
        assertEquals(ProjectTypeAnalyzer.TechnologyStack.NODEJS, result);
    }

    @Test
    void analyzeTechnology_PythonProject_ReturnsPython() throws IOException {
        // Créer un projet Python
        Files.write(tempDir.resolve("requirements.txt"),
            """
            flask==2.0.1
            requests==2.26.0
            """.getBytes()
        );

        var result = projectTypeAnalyzer.analyzeTechnology(tempDir.toString());
        assertEquals(ProjectTypeAnalyzer.TechnologyStack.PYTHON, result);
    }

    @Test
    void analyzeTechnology_UnknownProject_ReturnsUnknown() throws IOException {
        Files.write(tempDir.resolve("random.txt"), "Hello World".getBytes());

        assertEquals(ProjectTypeAnalyzer.TechnologyStack.UNKNOWN, projectTypeAnalyzer.analyzeTechnology(tempDir.toString()));
    }


    @Test
    void analyzeTechnology_EmptyDirectory_ReturnsUnknown() {
        assertEquals(ProjectTypeAnalyzer.TechnologyStack.UNKNOWN, projectTypeAnalyzer.analyzeTechnology(tempDir.toString()));

    }

    @Test
    void analyzeTechnology_NonExistentDirectory_ThrowsException() {
        Path nonExistentPath = tempDir.resolve("non-existent");
        assertThrows(IllegalArgumentException.class, () ->
            projectTypeAnalyzer.analyzeTechnology(nonExistentPath.toString())
        );
    }

    @Test
    void analyzeTechnology_MultipleProjectTypes_PrioritizesJava() throws IOException {
        // Créer un projet avec plusieurs marqueurs technologiques
        Files.write(tempDir.resolve("pom.xml"),
            """
            &lt;project&gt;
                &lt;modelVersion&gt;4.0.0&lt;/modelVersion&gt;
                &lt;groupId&gt;com.example&lt;/groupId&gt;
                &lt;artifactId&gt;test-project&lt;/artifactId&gt;
                &lt;version&gt;1.0.0&lt;/version&gt;
            &lt;/project&gt;
            """.getBytes()
        );
        Files.write(tempDir.resolve("package.json"),
            """
            {
                "name": "test-project",
                "version": "1.0.0"
            }
            """.getBytes()
        );

        var result = projectTypeAnalyzer.analyzeTechnology(tempDir.toString());
        assertEquals(ProjectTypeAnalyzer.TechnologyStack.JAVA_MAVEN, result);
    }
}
