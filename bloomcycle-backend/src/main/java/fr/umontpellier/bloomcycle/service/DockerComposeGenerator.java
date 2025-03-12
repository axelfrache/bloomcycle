package fr.umontpellier.bloomcycle.service;

import org.springframework.stereotype.Service;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import fr.umontpellier.bloomcycle.service.ProjectTypeAnalyzer.TechnologyStack;

@Service
public class DockerComposeGenerator {

    private static final String MAVEN_TEMPLATE = """
        services:
          app:
            build:
              context: .
              dockerfile: Dockerfile
            ports:
              - "8080:8080"
            volumes:
              - .:/app
        """;

    private static final String NODEJS_TEMPLATE = """
        version: '3.8'
        services:
          app:
            build:
              context: .
              dockerfile: Dockerfile
            ports:
              - "3000:3000"
            volumes:
              - ./:/app
              - /app/node_modules
            environment:
              - NODE_ENV=development
            command: npm start
        """;

    private static final String PYTHON_TEMPLATE = """
        services:
          app:
            build:
              context: .
              dockerfile: Dockerfile
            ports:
              - "5000:5000"
            volumes:
              - .:/app
        """;

    public void generateDockerCompose(String projectPath, TechnologyStack technology) throws IOException {
        String template = switch (technology) {
            case JAVA_MAVEN, JAVA_GRADLE -> MAVEN_TEMPLATE;
            case NODEJS -> NODEJS_TEMPLATE;
            case PYTHON -> PYTHON_TEMPLATE;
            case UNKNOWN -> throw new IllegalArgumentException("Unknown project type");
        };

        var dockerComposePath = Path.of(projectPath, "docker-compose.yml");
        Files.writeString(dockerComposePath, template);

        generateDockerfile(projectPath, technology);
    }

    private void generateDockerfile(String projectPath, TechnologyStack technology) throws IOException {
        var dockerfile = switch (technology) {
            case JAVA_MAVEN -> """
                FROM maven:3.8-openjdk-17
                WORKDIR /app
                COPY . .
                RUN mvn clean package
                CMD ["java", "-jar", "target/*.jar"]
                """;
            case NODEJS -> """
                FROM node:18-alpine
                
                WORKDIR /app
                
                # Install dependencies first (better layer caching)
                COPY package*.json ./
                RUN npm install
                
                # Then copy source code
                COPY . .
                
                # Expose port
                EXPOSE 3000
                
                # Start command is in docker-compose
                CMD ["npm", "start"]
                """;
            case PYTHON -> """
                FROM python:3.9
                WORKDIR /app
                COPY requirements.txt .
                RUN pip install -r requirements.txt
                COPY . .
                CMD ["python", "app.py"]
                """;
            default -> throw new IllegalArgumentException("Unknown project type");
        };

        var dockerfilePath = Path.of(projectPath, "Dockerfile");
        Files.writeString(dockerfilePath, dockerfile);
    }
} 