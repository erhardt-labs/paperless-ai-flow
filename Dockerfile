# Stage 1: Build stage with full JDK and Maven
FROM docker.io/library/maven:3.9-eclipse-temurin-21@sha256:9311699b501f3bad9643f5dd58365eadf7377eeacaa449f19851b087f144a019 AS build

# Set working directory
WORKDIR /app

# Copy pom files for dependency resolution
COPY pom.xml ./
COPY app/pom.xml ./app/
COPY paperless-ngx-client/pom.xml ./paperless-ngx-client/

# Pre-fetch dependencies to speed up subsequent builds
# (Tests are skipped here because we just want dependencies cached)
RUN mvn -B -q -DskipTests dependency:go-offline

# Copy source code
COPY app/src/ ./app/src/
COPY paperless-ngx-client/src/ ./paperless-ngx-client/src/

# Build the application JAR (skip tests for faster container builds)
# If you want tests, remove -DskipTests
RUN mvn -B -DskipTests package

# Stage 2: Runtime stage with minimal JRE
FROM docker.io/library/eclipse-temurin:21-jre@sha256:0a9f4e4a46a052522c092d4bc1ef9301362291d3e285bf5f1354b539644e1d1c AS runtime

# Set working directory
WORKDIR /app

# Create config directory
RUN mkdir -p /app/config

# Copy the built JAR from builder stage
COPY --from=build /app/app/target/*.jar app.jar
COPY /app/src/main/resources/application.yaml /app/config/application.yaml

# Allow passing extra JVM options at runtime
ENV JAVA_OPTS="-Dspring.config.location=file:/app/config/application.yaml -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -XX:+ExitOnOutOfMemoryError"

# Run as non-root
USER 1000:1000

# Start the app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
