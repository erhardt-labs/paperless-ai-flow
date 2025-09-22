---
Date: 2025-09-21
TaskRef: "Fix OpenAI API hanging on blank images in OcrExtractionModel"

Learnings:
- OpenAI Vision API (gpt-4o, gpt-4-vision-preview) can hang indefinitely when processing completely blank/white images with no content
- The API appears to send the request but never returns a response when given an empty image
- Solution: Pre-check images for emptiness by sampling pixels and comparing to first pixel RGB value
- Performance optimization: Sample every 10th pixel instead of checking all pixels to detect blank images efficiently
- Added `isImageEmpty()` method that returns true if all sampled pixels have the same color

Difficulties:
- Initial confusion about why the synchronous OpenAI call was hanging - suspected reactive programming issues but it was actually the blank image content causing the API to not respond

Successes:
- Efficient pixel sampling algorithm that balances performance with accuracy
- Clear logging when skipping empty images for debugging
- Proper JavaDoc documentation explaining the purpose of the empty image check

Improvements_Identified_For_Consolidation:
- General pattern: Always validate input content before making expensive external API calls
- OpenAI Vision API: Blank images cause indefinite hangs, need pre-filtering
- Performance pattern: Pixel sampling (every 10th pixel) for image analysis instead of full pixel iteration
---

---
Date: 2025-09-22
TaskRef: "Multi-module refactor with paperless-ngx-client split and full Flux/Mono adoption"

Learnings:
- Multi-module Maven structure provides excellent separation of concerns: `app` for business logic, `paperless-ngx-client` for external API integration
- Dedicated client modules enable independent testing, versioning, and potential reuse in other projects
- Project Reactor (Mono/Flux) throughout the entire stack creates consistent, non-blocking I/O patterns
- AbstractReactivePagedService pattern enables elegant handling of paginated API responses with backpressure
- Spring Boot's reactive WebClient integrates seamlessly with cached lookups and error handling
- Mapper pattern (DocumentMapper, TagMapper, etc.) cleanly separates API response entities from domain DTOs
- Multi-module structure enables proper dependency boundaries: app depends on paperless-ngx-client, not vice versa

Difficulties:
- Refactoring from blocking to reactive required careful analysis of all I/O boundaries
- Ensuring proper error propagation through Mono/Flux chains while maintaining meaningful error messages  
- Maven module dependency management required understanding of compile vs runtime scope for shared dependencies
- Converting synchronous service calls to reactive composition patterns required rethinking control flow

Successes:
- Clean module boundaries with paperless-ngx-client as a fully self-contained, testable module
- All Paperless API operations now return Mono/Flux, enabling proper reactive composition
- Improved testability through module isolation and reactive testing patterns
- Spring's caching works seamlessly with reactive types (Mono/Flux) for performance optimization
- Configuration and WebClient setup properly isolated in paperless-ngx-client module

Improvements_Identified_For_Consolidation:
- Multi-module Maven pattern: Split external integrations into dedicated client modules for better maintainability
- Reactive-first architecture: Use Mono/Flux throughout the stack, not just at HTTP boundaries
- AbstractReactivePagedService pattern: Reusable pattern for handling paginated reactive API responses
- Mapper separation: Keep API response entities separate from business domain DTOs using dedicated mappers
- Module dependency strategy: External integrations as dependencies of business logic, never the reverse
---

---
Date: 2025-09-22
TaskRef: "Docker containerization with multi-stage build for app module"

Learnings:
- Multi-stage Docker builds enable optimal layer caching: separate build stage with full JDK, runtime stage with minimal JRE
- Spring Boot Maven plugin must be properly enabled (uncommented) in app/pom.xml to create executable JAR
- Eclipse Temurin Alpine images provide excellent balance of size, security, and Java 21 support
- Container security best practices: non-root user execution, minimal base image, proper directory permissions
- JVM container optimizations: MaxRAMPercentage=75.0, +UseContainerSupport, +UseStringDeduplication for memory efficiency
- .dockerignore is crucial for build performance: exclude .git, target/, memory-bank/, and other unnecessary files
- Health check integration with Spring Boot Actuator provides proper container orchestration support

Difficulties:
- Initially forgot to uncomment Spring Boot Maven plugin in app/pom.xml, which would prevent executable JAR creation
- Required careful consideration of which packages are needed in runtime Alpine container (curl for health checks, fonts for PDF processing)

Successes:
- Comprehensive multi-stage Dockerfile with optimal layer caching
- Security-focused container configuration with non-root execution
- Production-ready labels and metadata for container management
- Efficient .dockerignore reducing build context size significantly

Improvements_Identified_For_Consolidation:
- Docker multi-stage pattern: Build stage with full JDK + Maven, runtime stage with minimal JRE for optimal size
- Container security: Always run as non-root user, create dedicated user/group with specific UIDs
- JVM containerization: Use MaxRAMPercentage instead of fixed heap sizes, enable container-specific optimizations
- Spring Boot containerization: Ensure spring-boot-maven-plugin is enabled for executable JAR creation
---
