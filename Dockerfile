# Stage 1: Build stage with full JDK and Maven
FROM docker.io/library/maven:3.9-eclipse-temurin-21@sha256:674ec814977fd05dc5cd3627802efb756351f27d4f029bc47b8a7b5156bb9231 AS build

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
FROM docker.io/library/eclipse-temurin:24-jre@sha256:8cb2387a28af84cf0db0948d9c67d4480192f4e567027a3963f145d218e8b4f2 AS runtime

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=build /app/app/target/*.jar app.jar

# Allow passing extra JVM options at runtime
ENV JAVA_OPTS=""

# Run as non-root
USER 1000:1000

# Start the app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
