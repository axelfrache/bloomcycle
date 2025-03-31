# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-23 AS maven_build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:23-jre

EXPOSE 8080

# Install Docker CLI
RUN apt-get update && \
    apt-get install -y apt-transport-https ca-certificates curl gnupg && \
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    chmod a+r /etc/apt/keyrings/docker.gpg && \
    echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian bookworm stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && \
    apt-get install -y docker-ce-cli && \
    rm -rf /var/lib/apt/lists/*

COPY --from=maven_build /app/target/*.jar /app/application.jar

# Create app directory and set permissions
RUN mkdir -p /app/projects && \
    chmod 755 /app/projects

CMD ["java", "-jar", "/app/application.jar"]