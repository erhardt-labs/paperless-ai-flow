# Tech Context: Paperless-AI Pipeline

## Core Technology Stack

### Language & Runtime
- **Java 21+** with modern language features
- **JVM optimizations** for container environments
- **GraalVM compatibility** consideration for native image builds (future enhancement)

### Frameworks & Libraries
- **Spring Boot 3.5.6** for application framework and auto-configuration
- **Spring Integration** for message-driven architecture and polling
- **Spring WebFlux** for reactive web stack and HTTP clients
- **Spring AI 1.0.2** with OpenAI integration for LLM/OCR providers
- **Spring Configuration Processor** for YAML configuration binding and IDE support
- **Project Reactor** (Mono/Flux) for reactive programming throughout the entire stack

### Multi-Module Architecture
- **Parent POM** with shared dependency management and Java 21 configuration
- **App Module (app/):** Main application with business logic, AI models, services, and Spring Integration
- **Paperless Client Module (paperless-ngx-client/):** Dedicated external API client with complete reactive implementation
- **Clear dependency boundaries:** App depends on paperless-ngx-client, ensuring proper separation of concerns
- **Independent testing:** Each module has its own test suite with appropriate scope and mocking

### Build & Packaging
- **Maven 3.9+** for dependency management and build lifecycle
- **Maven Wrapper** for consistent build environment
- **Docker multi-stage builds** for optimized container images
- **Jib Maven plugin** for efficient container image creation

### Testing Framework
- **JUnit 5** as primary testing framework
- **Testcontainers** for integration testing with real services
- **WireMock** for mocking external API calls (OCR, LLM providers)
- **Spring Boot Test** for application context testing
- **Mockito** for unit test mocking

## Data & Persistence

### State Management
- **No internal database** - all state maintained in Paperless-ngx
- **Document fingerprinting** using SHA-256 hashes stored as Paperless custom fields
- **Pipeline versioning** via configuration hash stored with documents
- **In-memory caching** for configuration and provider instances

### Configuration Management
- **YAML primary format** using SnakeYAML library
- **JSON Schema validation** for configuration structure
- **Environment variable substitution** using Spring's `${VAR:default}` syntax
- **Configuration hot-reload** via file watching (Spring Boot DevTools pattern)

## External Integrations

### Paperless-ngx API
- **Spring WebClient** for reactive HTTP client
- **JWT token authentication** with automatic refresh
- **Connection pooling** and timeout configuration
- **Error handling** with exponential backoff retry

### OCR Provider Integrations
- **Tesseract (local):** ProcessBuilder for command-line execution
- **Google Cloud Vision API:** Google Cloud client libraries
- **Azure Computer Vision:** Azure SDK for Java
- **ABBYY Cloud OCR SDK:** HTTP client with REST API

### LLM Provider Integrations
- **OpenAI API:** Custom HTTP client with Spring WebClient
- **Ollama (local):** HTTP client for REST API
- **Azure OpenAI Service:** Azure OpenAI client library
- **Anthropic Claude:** HTTP client implementation

## Development Environment

### Local Development Setup
```bash
# Prerequisites
- Java 21 (OpenJDK or Oracle)
- Maven 3.9+
- Docker & Docker Compose
- IDE with Spring Boot support (IntelliJ IDEA recommended)

# Development workflow (Implemented)
mvn spring-boot:run                    # Local development server with auto-restart
mvn test                              # Unit and integration tests
mvn clean compile                     # Build with Lombok and configuration processor
./mvnw spring-boot:run                # Via Maven wrapper
```

### Configuration Files (Implemented)
- **application.yml** - Spring Boot configuration with pipeline settings
- **pom.xml** - Complete Maven configuration with all dependencies
- **PipelineConfiguration.java** - Type-safe YAML configuration binding
- **WebClientConfiguration.java** - HTTP client configuration

### Configuration Files (Future)
- **pipeline-config.yml** - External pipeline definitions (separate from application.yml)
- **logback-spring.xml** - Structured logging configuration
- **docker-compose.yml** - Local development environment with Paperless-ngx

### IDE Configuration
- **Java 21** language level
- **Lombok plugin** for annotation processing
- **Spring Boot plugin** for application support
- **YAML schema validation** for configuration files

## Production Deployment

### Container Specifications
```dockerfile
# Multi-stage build
FROM eclipse-temurin:21-jre-alpine as runtime
# Optimized for size and security
# Non-root user execution
# Health check endpoint
```

### Orchestration Support
- **Docker Compose** for simple deployments
- **Kubernetes manifests** with Helm charts
- **ConfigMaps** for YAML configuration files
- **Secrets** for API keys and tokens

### Resource Requirements
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "200m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

## Technical Constraints

### Performance Constraints
- **Memory efficiency:** Process documents without loading entire files into memory
- **API rate limits:** Respect provider quotas and implement backoff
- **Concurrent processing:** Limit parallel operations to prevent resource exhaustion
- **Document size limits:** Handle large PDFs efficiently with streaming

### Security Constraints
- **No secrets in configuration:** All sensitive values via environment variables
- **API token management:** Secure storage and rotation support
- **Network security:** HTTPS-only communication with external services
- **Container security:** Non-root execution, minimal attack surface

### Operational Constraints
- **Stateless operation:** No local state that affects processing decisions
- **Graceful shutdown:** Complete in-flight processing before termination
- **Health monitoring:** Provide meaningful health and readiness endpoints
- **Log security:** No sensitive data in application logs

## Dependencies & Versions

### Core Dependencies (Implemented)
```xml
<properties>
    <java.version>21</java.version>
    <spring-ai.version>1.0.2</spring-ai.version>
</properties>

<!-- Core Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-integration</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Spring AI - Complete Implementation -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>

<!-- AI Processing Dependencies (NEW) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- PDF Processing - ICEpdf Integration -->
<dependency>
    <groupId>org.icepdf.os</groupId>
    <artifactId>icepdf-core</artifactId>
    <version>6.3.2</version>
</dependency>

<!-- File Utilities (NEW) -->
<dependency>
    <groupId>consulting.erhardt.paperless_ai_flow</groupId>
    <artifactId>utils</artifactId>
</dependency>
```

### Development & Configuration Dependencies (Implemented)
- **Spring Boot Configuration Processor:** `spring-boot-configuration-processor` - IDE support and metadata generation
- **Lombok:** `org.projectlombok:lombok` - Code generation and boilerplate reduction
- **Project Reactor:** Built-in with WebFlux - Reactive streams support

### Testing Dependencies (Implemented)
- **JUnit 5:** `spring-boot-starter-test` - Modern testing framework
- **Spring Integration Test:** `spring-integration-test` - Integration testing support
- **WireMock:** `com.github.tomakehurst:wiremock-jre8:2.35.0` - HTTP service mocking
- **Reactor Test:** `io.projectreactor:reactor-test` - Reactive streams testing

### Provider-Specific Dependencies (Future)
- **Google Cloud Vision:** `com.google.cloud:google-cloud-vision` (to be added)
- **Azure Computer Vision:** `com.azure:azure-ai-vision-computervision` (to be added)
- **JSON Schema Validation:** `com.networknt:json-schema-validator` (to be added)

## Tool Usage Patterns

### Maven Build Configuration (Implemented)
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </path>
                    <path>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-configuration-processor</artifactId>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Maven Profiles (Future Enhancement)
```xml
<profiles>
    <profile>
        <id>integration-test</id>
        <!-- Testcontainers and external service integration -->
    </profile>
    <profile>
        <id>docker-build</id>
        <!-- Jib container image creation -->
    </profile>
</profiles>
```

### Docker Compose Services
```yaml
services:
  paperless-ai-pipeline:
    build: .
    environment:
      - PAPERLESS_TOKEN=${PAPERLESS_TOKEN}
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    volumes:
      - ./config:/app/config:ro
```

### Kubernetes Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: paperless-ai-pipeline
spec:
  template:
    spec:
      containers:
      - name: pipeline
        image: paperless-ai-pipeline:latest
        envFrom:
        - secretRef:
            name: api-keys
        volumeMounts:
        - name: config
          mountPath: /app/config
```

## Future Technical Considerations

### Scalability Enhancements
- **Kafka/RabbitMQ** for message-driven architecture
- **Redis** for distributed caching and coordination
- **Database** for processing history and analytics (optional)

### Performance Optimizations
- **GraalVM native image** for faster startup and lower memory usage
- **Connection pooling** optimization for provider APIs
- **Async processing** with reactive streams throughout

### Security Enhancements
- **OAuth2/OIDC** integration for enterprise authentication
- **Secrets management** integration (HashiCorp Vault, AWS Secrets Manager)
- **Network policies** and service mesh integration
