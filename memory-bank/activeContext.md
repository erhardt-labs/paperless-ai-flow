# Active Context: Paperless-AI Pipeline

## Current Work Focus

### Phase: Document Update Implementation - Final Pipeline Integration
- **Status:** Document patching functionality implemented with custom serialization for Paperless API
- **Just Completed:** DocumentPatchRequest entity, MapAsArraySerializer, and comprehensive API integration testing
- **Current Priority:** Final pipeline integration connecting AI extraction results with document updates

### Major Achievements (NEW)
1. **✅ Complete AI Metadata Extraction Framework - COMPLETE**
   - ✅ AbstractAiModel<T> template method pattern for consistent AI processing
   - ✅ Five specialized extraction models: Title, Tags, Correspondent, CustomFields, CreatedDate
   - ✅ DocumentMetadataExtractionService with parallel processing using Mono.zip()
   - ✅ JSON Schema-driven structured output from OpenAI with validation
   - ✅ Resource-based prompt templates and schemas for each extraction type
   - ✅ Comprehensive error handling with Optional-based graceful degradation

2. **✅ Spring Integration Pipeline - COMPLETE**
   - ✅ DocumentPollingIntegrationConfig with end-to-end workflow orchestration
   - ✅ @Scheduled polling with @ServiceActivator pattern for automated processing
   - ✅ Channel-based message flow: pollingChannel → metadataExtractChannel → metadataResultChannel
   - ✅ QueueChannel (buffering) vs DirectChannel (immediate processing) optimization
   - ✅ Message headers for pipeline context (pipeline definition, pipeline name)
   - ✅ Service activator error handling with null return for flow termination
   - ✅ Complete integration: polling → OCR → AI metadata extraction → result handling

3. **✅ Spring AI Integration - COMPLETE**
   - ✅ OpenAiChatModel integration with OpenAiChatOptions configuration
   - ✅ Structured JSON output using ResponseFormat.Type.JSON_SCHEMA
   - ✅ Reactive processing with Schedulers.boundedElastic() for blocking AI calls
   - ✅ Template-based prompt engineering with system and user message separation

4. **✅ Configuration-Driven Processing - COMPLETE**  
   - ✅ PipelineDefinition.Extraction with boolean flags for selective AI processing
   - ✅ Resource loading via FileUtils.readFileFromResources()
   - ✅ Model configuration with configurable model selection per extraction type

5. **✅ Reactive Utility Patterns - COMPLETE**
   - ✅ PatchOps.applyIfPresent() for elegant conditional reactive operations
   - ✅ BiFunction<M, T, M> pattern for conditional document enrichment
   - ✅ Mono composition utilities for cleaner reactive code

6. **✅ Document Update Infrastructure - COMPLETE**
   - ✅ DocumentPatchRequest entity with proper JSON serialization
   - ✅ MapAsArraySerializer for Paperless-ngx custom field format compatibility
   - ✅ Enhanced PaperlessNgxApiClient with patchDocument functionality
   - ✅ Comprehensive integration testing with WireMock and JSON schema validation
   - ✅ Document mapper integration for converting AI extraction results to patch requests

### Immediate Next Steps
1. **Final Pipeline Integration**
   - Connect DocumentMetadataExtractionService results with DocumentPatchRequest creation
   - Add final Spring Integration channel for document update operations
   - Implement DocumentFieldPatchingService integration with patch request building
   - Complete end-to-end workflow: poll → OCR → AI extraction → document patch → result logging

2. **Production Readiness & Deployment**
   - Add comprehensive error handling and retry logic for document update operations
   - Implement idempotency tracking to prevent duplicate processing
   - Add structured logging with correlation IDs throughout the complete pipeline
   - Add metrics collection for end-to-end processing performance

## Active Decisions & Considerations

### Architecture Decisions Made
- **Provider Pattern** for OCR/LLM abstraction - enables easy provider switching
- **Pipeline Pattern** for step chaining - provides flexibility and reusability
- **Stateless Design** - all state in Paperless-ngx, enables horizontal scaling
- **YAML Configuration** - declarative, user-friendly, version-controllable

### Design Preferences Established
- **Java 21 features** - use modern language constructs (var, records consideration vs Lombok)
- **Lombok over Records** - as specified in code guidelines, use `@Builder` and `@Value`
- **Reactive Programming** - use Project Reactor where beneficial, especially for I/O operations
- **Spring Boot conventions** - follow Spring Boot auto-configuration patterns

### Active Technical Decisions Made
1. **Configuration Framework Architecture**
   - **Decision:** Spring Boot @ConfigurationProperties with Lombok @Value/@Builder pattern
   - **Implementation:** PipelineConfiguration with nested configuration classes
   - **Rationale:** Type-safe configuration binding with immutable objects, follows code guidelines

2. **Reactive API Client Pattern**
   - **Decision:** Spring WebClient with Mono/Flux reactive types
   - **Implementation:** PaperlessApiClient with token authentication and reactive document querying
   - **Rationale:** Non-blocking I/O for better resource utilization, integrates with Spring Boot patterns

3. **OCR Provider Architecture**
   - **Decision:** Interface-based design with Spring AI integration
   - **Implementation:** OcrClient interface with OpenAiOcrClient (simulated pending vision API support)
   - **Rationale:** Enables provider flexibility while leveraging Spring AI ecosystem

4. **Testing Strategy Implementation**
   - **Decision:** Comprehensive testing with WireMock and reactor-test
   - **Implementation:** Integration tests with external API mocking, reactive stream testing
   - **Rationale:** Ensures reliability without external dependencies in CI/CD

### Open Technical Decisions
1. **Document State Management**
   - Decision needed: Custom fields vs tags for tracking processed documents
   - Preference: Custom fields for structured metadata, tags for user-visible state

2. **Pipeline Orchestration Pattern**
   - Decision needed: Spring Integration vs custom pipeline engine
   - Preference: Custom lightweight engine for simplicity and control

3. **Configuration Hot-Reload**
   - Decision needed: File watching vs manual reload endpoint
   - Preference: File watching with Spring Boot DevTools pattern

## Important Patterns & Preferences

### Code Organization Patterns
**Multi-Module Maven Structure (IMPLEMENTED):**
```
paperless-ai-flow/
├── app/                           # Main application module
│   └── src/main/java/.../app/
│       ├── ai/                    # AI extraction models (Title, Tag, Correspondent, CustomField)
│       ├── config/                # Application configuration (Pipeline, WebClient)
│       ├── integration/           # Spring Integration flows (DocumentPollingIntegration)
│       ├── ocr/                   # OCR processing (OcrClient, PdfOcrService)
│       ├── service/               # Business logic services (DocumentPolling, MetadataExtraction)
│       └── paperless/             # Legacy paperless code (to be removed)
├── paperless-ngx-client/         # External API client module
│   └── src/main/java/.../paperless_ngx/client/
│       ├── configs/               # WebClient configuration, caching
│       ├── dtos/                  # Business domain objects (Document, Tag, Correspondent)
│       ├── entities/              # API response entities (DocumentResponse, TagResponse)
│       ├── mappers/               # Entity-to-DTO conversion (DocumentMapper, TagMapper)
│       ├── services/              # Reactive API services (DocumentService, TagService)
│       └── utils/                 # Pagination utilities
└── pom.xml                        # Parent POM with shared configuration
```

**Module Responsibilities:**
- **app:** Business logic, pipeline orchestration, AI processing, configuration management
- **paperless-ngx-client:** External API integration, reactive services, response mapping, caching

### Configuration Patterns
- **Environment variable injection** using Spring's `${VAR:default}` syntax
- **Nested configuration objects** with proper validation annotations
- **Provider registration** via configuration rather than classpath scanning

### Error Handling Patterns
- **Typed exceptions** for different error categories (ConfigurationError, ProviderError, PipelineError)
- **Correlation IDs** throughout the processing chain for traceability
- **Graceful degradation** when non-critical steps fail

## Key Learnings & Insights

### From Implementation Phase
1. **Spring Boot Configuration Processor** generates metadata for IDE support and validation
2. **Reactive WebClient** requires different error handling patterns than traditional REST clients
3. **Spring AI integration** provides clean abstraction but vision API support is still evolving
4. **PDF processing with PDFBox** enables efficient image conversion for OCR processing
5. **Lombok @Value with @Builder** pattern works excellently for immutable configuration objects

### Technical Implementation Insights
1. **Reactor pattern consistency** throughout the stack improves resource efficiency and code coherence
2. **Token-based authentication** with Paperless API works seamlessly with Spring Security patterns
3. **WireMock integration testing** enables reliable external API testing without network dependencies
4. **Configuration nesting** with Spring Boot @ConfigurationProperties enables clean YAML structure
5. **Interface segregation** for OCR providers enables easy testing and future provider additions

### Architecture Validation
1. **Package structure alignment** with domain concepts improves code organization and maintainability
2. **Service layer separation** enables clean testing boundaries and component isolation
3. **Reactive stream composition** in PaperlessApiClient enables efficient data processing pipelines
4. **Configuration-driven approach** already showing flexibility benefits in testing scenarios

## Current Environment Context

### Project State
- **Comprehensive Maven project** with Spring Boot 3.5.6, Java 21, all major dependencies configured
- **Complete package structure** following established domain-driven patterns
- **Production-ready configuration** with YAML-based pipeline definitions
- **Full test suite** with integration testing, WireMock, and reactive stream testing

### Implementation Status
- **Configuration framework:** ✅ Complete (PipelineConfiguration with full YAML binding)
- **Paperless API client:** ✅ Complete (PaperlessApiClient with reactive document/tag operations)
- **OCR infrastructure:** ✅ Complete (OcrClient interface, OpenAI implementation ready for vision API)
- **PDF processing:** ✅ Complete (PdfOcrService with image conversion)
- **Service layer:** ✅ Complete (DocumentPollingService, comprehensive business logic)
- **Integration testing:** ✅ Complete (Full test coverage with external API mocking)

### Current Session Focus Areas
1. **Pipeline Integration** - connect DocumentPollingService with OCR processing for end-to-end workflow
2. **Document Workflow** - implement complete poll → download → OCR → update cycle
3. **Error Handling** - add comprehensive retry logic and error recovery patterns
4. **Observability** - add structured logging, correlation IDs, and metrics collection

## Implementation Priority Queue
1. **High Priority:** Configuration framework and validation
2. **High Priority:** Paperless API client with authentication
3. **Medium Priority:** Provider interfaces and basic implementations
4. **Medium Priority:** Pipeline engine with step execution
5. **Lower Priority:** Advanced features (metrics, health checks, container optimization)

## Dependencies & Integration Points
- **Spring Boot 3.2+** for core framework
- **Spring WebClient** for HTTP client (Paperless API, provider APIs)
- **SnakeYAML** for configuration parsing
- **JSON Schema Validator** for configuration validation
- **Lombok** for reducing boilerplate code
- **JUnit 5 + Testcontainers** for comprehensive testing

## Critical API Integration Knowledge
- **Paperless API Reference** documented in `memory-bank/paperless_api_reference.md`
- **Token Authentication** required for headless operation
- **Custom Field Queries** essential for idempotency and selective processing
- **Bulk Operations** enable efficient metadata updates
- **API Versioning** (v9) with backward compatibility considerations
- **Task Monitoring** needed for document upload/consumption tracking
