# System Patterns: Paperless-AI Pipeline

## Architecture Overview

### Multi-Module Microservice Architecture
- **Main Application Module (app):** Business logic, pipeline orchestration, document processing services
- **Paperless Client Module (paperless-ngx-client):** Dedicated external API integration with complete reactive implementation
- **Stateless design** with all state maintained in Paperless-ngx
- **Reactive-first architecture** using Project Reactor (Mono/Flux) throughout the entire stack
- **Event-driven processing** via polling and pipeline execution
- **Container-native** deployment model

### Module Structure & Boundaries
```
paperless-ai-flow/
├── app/                           # Main application module
│   ├── src/main/java/.../app/
│   │   ├── ai/                    # AI extraction models
│   │   ├── config/                # Application configuration
│   │   ├── integration/           # Spring Integration flows
│   │   ├── service/               # Business logic services
│   │   └── ocr/                   # OCR processing
│   └── pom.xml                    # App dependencies
├── paperless-ngx-client/         # External API client module
│   ├── src/main/java/.../paperless_ngx/client/
│   │   ├── configs/               # WebClient & cache configuration
│   │   ├── dtos/                  # Business domain DTOs
│   │   ├── entities/              # API response entities
│   │   ├── mappers/               # Entity-to-DTO mappers
│   │   ├── services/              # Reactive API services
│   │   └── utils/                 # Pagination utilities
│   └── pom.xml                    # Client dependencies
└── pom.xml                        # Parent POM
```

### YAML-Driven Configuration
- **Declarative configuration** defining sources, providers, and processing pipelines
- **Runtime reconfiguration** via file watching or service restart
- **Environment variable injection** for sensitive values (API keys, tokens)
- **Schema validation** ensuring configuration correctness at startup
- **Module-specific configuration** isolated within respective modules

## Core Design Patterns

### Provider Pattern
**Purpose:** Abstract different OCR and LLM services behind common interfaces

```
OcrProvider interface
├── TesseractProvider
├── GoogleVisionProvider
├── AzureOcrProvider
└── AbbyySdkProvider

LlmProvider interface
├── OpenAiProvider
├── OllamaProvider
├── AzureOpenAiProvider
└── AnthropicProvider
```

**Benefits:**
- Easy provider switching via configuration
- Extensible for new providers
- Testable with mock implementations
- Provider-specific optimizations

### Pipeline Pattern
**Purpose:** Chain processing steps in configurable sequences

```
Pipeline
└── List<PipelineStep>
    ├── OcrStep
    ├── LlmExtractionStep
    ├── NormalizationStep
    └── PaperlessUpdateStep
```

**Key Characteristics:**
- **Composable:** Steps can be mixed and matched
- **Reusable:** Same steps used across different sources
- **Contextual:** Each step receives and enriches a document context
- **Fault-tolerant:** Steps can be retried independently

### Document Context Pattern
**Purpose:** Maintain processing state and metadata throughout the pipeline

```
DocumentContext {
  documentId: Long
  originalContent: byte[]
  extractedText: String
  metadata: Map<String, Object>
  processingHistory: List<StepResult>
  pipelineVersion: String
  documentHash: String
}
```

### AI Extraction Framework Pattern (NEW)
**Purpose:** Provide consistent, template-based AI processing for document metadata extraction

**Core Architecture:**
```
AbstractAiModel<T> {
  // Template method pattern for AI processing
  process(String content): T
  // Abstract methods for customization
  getSystemPrompt(): String
  getUserPrompt(String content): String  
  getJsonSchema(): String
  getResponseClass(): Class<T>
  getDefaultModel(): String
}

Concrete implementations:
├── TitleExtractionModel extends AbstractAiModel<TitleDto>
├── TagExtractionModel extends AbstractAiModel<TagsDto>
├── CorrespondentExtractionModel extends AbstractAiModel<CorrespondentDto>
└── CustomFieldExtractionModel extends AbstractAiModel<CustomFieldsDto>
```

**Key Features:**
- **Template Method Pattern:** Consistent processing flow with customizable extraction logic
- **JSON Schema Validation:** OpenAI ResponseFormat.Type.JSON_SCHEMA ensures structured output
- **Resource-based Configuration:** Prompts in `src/main/resources/prompts/`, schemas in `src/main/resources/schemas/`
- **Model Selection:** Configurable model per extraction type (e.g., "openai/o4-mini")
- **Error Handling:** Optional-based graceful degradation when AI processing fails

### Parallel AI Processing Pattern (NEW)
**Purpose:** Optimize AI metadata extraction through parallel processing

**Implementation in DocumentMetadataExtractionService:**
```java
// Run AI extractions in parallel when enabled
var titleMono = extraction.isTitle() ? 
  extractTitle(content) : Mono.just(Optional.empty());
var tagsMono = extraction.isTags() ?
  extractTags(content) : Mono.just(Optional.empty());
var correspondentMono = extraction.isCorrespondent() ?
  extractCorrespondent(content) : Mono.just(Optional.empty());
var customFieldsMono = extraction.isCustomFields() ?
  extractCustomFields(content) : Mono.just(Optional.empty());

return Mono.zip(titleMono, tagsMono, correspondentMono, customFieldsMono)
  .map(results -> buildDocumentMetadata(results));
```

**Benefits:**
- **Performance:** Parallel AI calls reduce total processing time
- **Configuration-driven:** Extraction types controlled by boolean flags
- **Fault tolerance:** Individual extraction failures don't break entire process
- **Resource efficiency:** Uses Schedulers.boundedElastic() for blocking AI calls

### Resource-Based Configuration Pattern (NEW)
**Purpose:** Externalize AI prompts and schemas for maintainability and flexibility

**Structure:**
```
src/main/resources/
├── prompts/                    # AI prompt templates
│   ├── title.md               # Title extraction prompt
│   ├── tags.md                # Tag extraction prompt
│   ├── correspondent.md       # Correspondent extraction prompt
│   ├── custom-fields.md       # Custom fields extraction prompt
│   └── ocr.md                 # OCR processing prompt
└── schemas/                   # JSON Schema definitions
    ├── title.json             # Title response schema
    ├── tags.json              # Tags response schema
    ├── correspondent.json     # Correspondent response schema
    └── custom-fields.json     # Custom fields response schema
```

**Implementation:**
- **FileUtils.readFileFromResources()** for loading prompt templates and schemas
- **JSON Schema enforcement** via OpenAI ResponseFormat for structured output
- **Version control friendly:** Plain text files enable easy prompt engineering
- **Environment separation:** Different prompts per environment possible
- **Default prompt handling:** PdfOcrService loads default from prompts/ocr.md when configuration prompt is null

### Spring AI Integration Pattern (NEW)
**Purpose:** Leverage Spring AI framework for consistent LLM integration

**Core Components:**
```java
OpenAiChatModel openAiChatModel           // Spring AI chat client
OpenAiChatOptions.builder()               // Request configuration
  .model(getDefaultModel())               // Model selection
  .responseFormat(new ResponseFormat(     // Structured output
    ResponseFormat.Type.JSON_SCHEMA, 
    getJsonSchema()))
  .build();

Prompt prompt = new Prompt(               // Message composition
  List.of(systemMessage, userMessage), 
  chatOptions);
```

**Benefits:**
- **Framework consistency:** Leverages Spring Boot auto-configuration
- **Structured output:** JSON Schema validation ensures reliable data extraction
- **Model flexibility:** Easy switching between OpenAI models
- **Future extensibility:** Spring AI abstracts provider-specific details

### Spring Integration Pipeline Pattern (NEW)
**Purpose:** Orchestrate document processing workflow using message-driven architecture

**Core Architecture:**
```java
@Configuration
@EnableIntegration
@IntegrationComponentScan
@EnableScheduling
public class DocumentPollingIntegrationConfig {
  
  @Scheduled(fixedRate = 30000)           // Automated polling
  public void pollDocuments() { ... }
  
  @ServiceActivator(                      // Processing steps
    inputChannel = "pollingChannel",
    outputChannel = "metadataExtractChannel"
  )
  public Message<Document> processStep(Message<Document> message) { ... }
}
```

**Channel Architecture:**
```
pollingChannel (QueueChannel)          // Buffering for document intake
    ↓
metadataExtractChannel (DirectChannel) // Immediate OCR processing
    ↓  
metadataResultChannel (DirectChannel)  // Immediate AI processing
    ↓
Final result handling
```

**Key Components:**
- **@Scheduled polling:** Automated document discovery with configurable intervals
- **@ServiceActivator pattern:** Step-by-step processing with clear input/output channels
- **Message headers:** Pipeline context preservation across processing steps
- **Channel types:** QueueChannel for buffering, DirectChannel for immediate processing
- **Error handling:** Return null to terminate message flow on processing failures

**Benefits:**
- **Decoupling:** Each processing step is independent and testable
- **Error isolation:** Failures in one document don't affect others
- **Scalability:** Message channels provide natural backpressure handling
- **Observability:** Message flow enables comprehensive logging and monitoring

### Reactive Utility Patterns (NEW)
**Purpose:** Provide reusable reactive composition utilities for conditional operations

**PatchOps Pattern:**
```java
@UtilityClass
public class PatchOps {
  public <T, M> Mono<M> applyIfPresent(
    @NonNull Mono<M> current,
    @NonNull Mono<T> source, 
    @NonNull BiFunction<M, T, M> applier
  ) {
    return current.flatMap(cur -> source
      .map(val -> applier.apply(cur, val))
      .defaultIfEmpty(cur)
    );
  }
}
```

**Usage Example:**
```java
// Apply metadata extraction results conditionally
return PatchOps.applyIfPresent(
  Mono.just(document),
  extractedMetadata,
  (doc, metadata) -> doc.toBuilder().title(metadata.getTitle()).build()
);
```

**Benefits:**
- **Conditional operations:** Clean reactive composition for optional value application
- **Type safety:** Generic types ensure compile-time correctness
- **Reusability:** Common pattern extracted to utility class
- **Readability:** More expressive than nested flatMap/map chains

### Reactive Programming Pattern
**Purpose:** Provide consistent, non-blocking I/O throughout the entire application stack using Project Reactor

**Core Implementation:**
- **Service Layer:** All methods return `Mono<T>` for single values, `Flux<T>` for streams
- **API Client:** Paperless-ngx-client module uses reactive WebClient exclusively
- **Error Handling:** Reactive error operators (`onErrorMap`, `onErrorResume`, `onErrorReturn`)
- **Composition:** Chain operations with `flatMap`, `map`, `filter`, `zipWith`

**Key Components:**
```
AbstractReactivePagedService<T> {
  // Handles paginated API responses with backpressure
  getAllItems(): Flux<T>
  getPage(pageNumber): Mono<PagedResponse<T>>
}

DocumentService extends AbstractReactivePagedService<Document>
TagService extends AbstractReactivePagedService<Tag>
CorrespondentService extends AbstractReactivePagedService<Correspondent>
CustomFieldsService extends AbstractReactivePagedService<CustomField>
```

**Benefits:**
- **Non-blocking I/O:** Better resource utilization under high load
- **Backpressure handling:** Natural streaming with demand-based flow control
- **Composability:** Easy chaining of async operations
- **Testing:** StepVerifier enables deterministic async testing
- **Error propagation:** Maintains context through reactive chains

### Idempotency Pattern
**Purpose:** Ensure documents are processed exactly once per pipeline version

**Implementation:**
- **Document fingerprinting** using content hash
- **Pipeline versioning** to detect configuration changes
- **Processing markers** stored as Paperless tags or custom fields
- **Skip logic** for already-processed documents

## Component Relationships

### Core Components
```
PipelineEngine
├── depends on: ConfigurationService
├── depends on: DocumentPollingService
├── depends on: PipelineStepFactory
└── manages: PipelineExecutor

PipelineExecutor
├── depends on: ProviderRegistry
├── depends on: PaperlessApiClient
└── executes: PipelineStep implementations

ProviderRegistry
├── manages: OcrProvider instances
├── manages: LlmProvider instances
└── provides: Provider resolution by name
```

### Data Flow
1. **ConfigurationService** loads and validates YAML configuration
2. **DocumentPollingService** queries Paperless API for matching documents
3. **PipelineEngine** orchestrates pipeline execution per source configuration
4. **PipelineExecutor** runs individual steps with appropriate providers
5. **PaperlessApiClient** updates documents with enriched metadata

## Critical Implementation Paths

### Configuration Loading Path
```
Startup → YamlLoader → SchemaValidator → ConfigurationService → ProviderRegistry
```

### Document Processing Path
```
Poll → Filter → Download → Pipeline → OCR → LLM → Normalize → Update → Tag
```

### Error Handling Path
```
Exception → RetryPolicy → Backoff → Log → Metrics → (Dead Letter Queue)
```

## Extensibility Points

### Adding New Providers
1. Implement `OcrProvider` or `LlmProvider` interface
2. Register in `ProviderRegistry` with configuration mapping
3. Add provider-specific configuration schema
4. Update YAML validation rules

### Adding New Pipeline Steps
1. Implement `PipelineStep` interface
2. Register in `PipelineStepFactory`
3. Define step-specific configuration schema
4. Add to YAML step type enumeration

### Configuration Schema Extension
1. Extend YAML schema definitions
2. Update configuration POJOs
3. Add validation rules
4. Document new configuration options

## Observability Patterns

### Structured Logging
- **Correlation IDs** for tracing document processing
- **Contextual fields** (documentId, pipelineStep, provider)
- **Consistent log levels** and message formats

### Metrics Collection
- **Processing counters** (documents processed, failed, skipped)
- **Timing metrics** (OCR duration, LLM response time, end-to-end processing)
- **Provider metrics** (API calls, token usage, error rates)
- **Pipeline metrics** (step success rates, retry counts)

### Health Checks
- **Readiness probe:** Configuration loaded, providers initialized
- **Liveness probe:** Service responsive, not deadlocked
- **Dependency checks:** Paperless API connectivity, provider availability

## Security Patterns

### Secrets Management
- **Environment variable injection** for sensitive configuration
- **No secrets in YAML** files or application logs
- **Secure defaults** for API timeouts and retry policies

### API Security
- **Token-based authentication** for Paperless API
- **Provider-specific security** (API keys, OAuth tokens)
- **Request/response sanitization** to prevent data leaks in logs

## Performance Patterns

### Parallel Processing
- **Source-level parallelization** for multiple document streams
- **Provider connection pooling** for efficient API usage
- **Async processing** where supported by providers

### Resource Management
- **Memory-efficient document handling** with streaming where possible
- **Provider rate limiting** to respect API quotas
- **Graceful degradation** under resource constraints
