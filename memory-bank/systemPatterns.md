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

### Spring Integration Pipeline Pattern with Advanced Queueing (UPDATED)
**Purpose:** Orchestrate document processing workflow using message-driven architecture with intelligent queue management

**Enhanced Queueing Architecture:**
```java
@Configuration
@EnableIntegration
@IntegrationComponentScan
@EnableScheduling
public class DocumentPollingIntegrationConfig {
  
  @Scheduled(fixedRate = 30000)           // Automated polling with capacity awareness
  public void pollDocuments() {
    var remainingCapacity = pollingChannel.getRemainingCapacity();
    if (remainingCapacity <= 0) {
      log.debug("Polling channel full, skip for pipeline '{}'", pipeline.getName());
      continue;
    }
    
    // Smart enqueueing with lock management
    docsFlux
      .filter(doc -> documentLockRegistry.tryLock(doc.getId()))  // Document-level locking
      .handle((doc, sink) -> {
        var accepted = pollingChannel.send(message, 0);          // Non-blocking send
        if (!accepted) {
          documentLockRegistry.unlock(doc.getId());              // Cleanup on failure
        }
      })
      .take(remainingCapacity)                                   // Respect queue capacity
      .collectList().block();
  }
}
```

**Advanced Channel Architecture:**
```
pollingChannel (QueueChannel, capacity=25)    // Smart buffering with capacity management
    ↓
metadataExtractChannel (DirectChannel)         // Immediate OCR processing  
    ↓
metadataResultChannel (DirectChannel)          // Immediate AI processing
    ↓
finishedDocumentChannel (DirectChannel)        // Final document updates
```

**Enhanced Queue Management Features:**
- **Capacity-Aware Polling:** Checks remainingCapacity before attempting to enqueue documents
- **Non-Blocking Queue Operations:** Uses send(message, 0) for immediate acceptance/rejection
- **Document-Level Locking:** IdLockRegistryService prevents duplicate processing across threads
- **Smart Lock Cleanup:** Automatically unlocks documents when queue is full or processing fails
- **Backpressure Handling:** take(remainingCapacity) limits documents processed per cycle
- **Pipeline-Specific Processing:** Each pipeline respects shared queue capacity limits

**Key Components:**
- **@Scheduled polling:** Automated document discovery with smart capacity management
- **@ServiceActivator pattern:** Step-by-step processing with comprehensive error isolation
- **Message headers:** Pipeline context preservation with pipeline definition and name
- **QueueChannel configuration:** Fixed capacity (25) with proper ComponentName setting
- **Reactive document querying:** Flux-based document retrieval with streaming pagination
- **Lock registry integration:** Document-level concurrency control with automatic cleanup

**Advanced Error Handling:**
```java
// Document unlocking on processing failure
private void unlockDocument(Message<Document> message) {
  var lockedId = message.getPayload().getId();
  documentLockRegistry.unlock(lockedId);
}

// Service activator error handling
@ServiceActivator(inputChannel = "pollingChannel", outputChannel = "metadataExtractChannel")
public Message<Document> processDocumentOcr(Message<Document> message) {
  try {
    // Processing logic
    return processedMessage;
  } catch (Exception e) {
    log.error("Error processing: {}", e.getMessage(), e);
    unlockDocument(message);
    return null;  // Terminate message flow
  }
}
```

**Benefits:**
- **Intelligent Backpressure:** Queue capacity awareness prevents system overload
- **Concurrent Processing Safety:** Document-level locking prevents race conditions
- **Resource Efficiency:** Non-blocking operations with proper cleanup
- **Pipeline Isolation:** Each pipeline respects shared resources while maintaining independence
- **Fault Tolerance:** Comprehensive error recovery with automatic resource cleanup
- **Observability:** Detailed logging of queue status, capacity, and processing decisions

### PDF Processing Pattern (UPDATED - ICEpdf Integration)
**Purpose:** Convert PDF documents to images for OCR processing using ICEpdf library

**Core Architecture:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfOcrService {
  
  private static final float TARGET_DPI = 300f;
  private static final float RENDER_SCALE = TARGET_DPI / 72f; // ICEpdf uses 72 DPI baseline
  private static final float RENDER_ROTATION = 0f;
  private static final float JPEG_COMPRESSION_QUALITY = 0.9f;

  public Mono<String> processDocument(Document document, PipelineDefinition pipelineDefinition) {
    return documentService.downloadById(document.getId())
      .flatMapMany(pdfBytes -> convertPdfToImages(pdfBytes, documentId))
      .collectList()
      .flatMap(images -> processImagesWithOcr(Flux.fromIterable(images), pipelineDefinition));
  }
}
```

**ICEpdf Integration Pattern:**
```java
private Flux<BufferedImage> convertPdfToImages(byte[] pdfBytes, @NonNull Integer documentId) {
  return Mono.fromCallable(() -> {
    var iceDocument = new org.icepdf.core.pobjects.Document();
    try (var bais = new ByteArrayInputStream(pdfBytes)) {
      iceDocument.setInputStream(bais, null);
      
      var pageCount = iceDocument.getNumberOfPages();
      
      return IntStream.range(0, pageCount)
        .mapToObj(pageIndex -> renderPageToImage(iceDocument, pageIndex))
        .toList();
    } catch (PDFSecurityException e) {
      throw new RuntimeException("Cannot process encrypted or password-protected PDF", e);
    } finally {
      iceDocument.dispose();
    }
  })
  .flatMapMany(Flux::fromIterable)
  .subscribeOn(Schedulers.boundedElastic());
}

private BufferedImage renderPageToImage(@NonNull org.icepdf.core.pobjects.Document document, int pageNumber) {
  var page = document.getPageTree().getPage(pageNumber);
  page.init();

  PDimension size = page.getSize(Page.BOUNDARY_CROPBOX, RENDER_ROTATION, RENDER_SCALE);
  var width = Math.max(1, (int) size.getWidth());
  var height = Math.max(1, (int) size.getHeight());

  var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
  var g2 = image.createGraphics();
  try {
    // Improve text/line quality in rasterization
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    page.paint(g2, GraphicsRenderingHints.PRINT, Page.BOUNDARY_CROPBOX, RENDER_ROTATION, RENDER_SCALE);
  } finally {
    g2.dispose();
  }
  return image;
}
```

**Key Features:**
- **ICEpdf integration:** Using org.icepdf.core.pobjects.Document instead of PDFbox
- **High-quality rendering:** 300 DPI output with antialiasing for optimal OCR results
- **Memory management:** Proper resource disposal with try-finally blocks
- **Error handling:** Specific handling for encrypted/password-protected PDFs
- **Reactive processing:** Non-blocking PDF conversion using Schedulers.boundedElastic()
- **Image optimization:** JPEG compression with 0.9 quality for size/quality balance

**Benefits:**
- **Better rendering quality:** ICEpdf often produces superior image quality compared to PDFbox
- **Memory efficiency:** Streaming page processing prevents memory issues with large PDFs
- **Thread safety:** ICEpdf Document instances properly isolated per processing thread
- **Error resilience:** Graceful handling of corrupted or encrypted PDFs
- **Performance optimization:** Page-by-page processing enables backpressure handling

### Document Update Pattern (NEW)
**Purpose:** Handle document metadata updates with custom API serialization for Paperless-ngx integration

**Core Architecture:**
```java
@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentPatchRequest {
  @JsonProperty("title")
  String title;

  @JsonProperty("created")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  LocalDate created;

  @JsonProperty("correspondent")
  Integer correspondentId;

  @JsonProperty("tags")
  List<Integer> tagIds;

  @JsonProperty("custom_fields")
  @JsonSerialize(using = MapAsArraySerializer.class)
  Map<Integer, String> customFields;

  @NonNull
  @JsonProperty("remove_inbox_tags")
  @Builder.Default
  Boolean removeInboxTags = false;
}
```

**Custom Serialization Pattern:**
```java
public class MapAsArraySerializer extends JsonSerializer<Map<Integer, String>> {
  @Override
  public void serialize(Map<Integer, String> value, JsonGenerator gen, 
                       SerializerProvider serializers) throws IOException {
    gen.writeStartArray();
    for (var entry : value.entrySet()) {
      gen.writeStartObject();
      gen.writeNumberField("field", entry.getKey());
      gen.writeStringField("value", entry.getValue());
      gen.writeEndObject();
    }
    gen.writeEndArray();
  }
}
```

**Key Features:**
- **@JsonInclude(NON_NULL):** Only serialize non-null fields for partial updates
- **Custom serialization:** MapAsArraySerializer converts Map<Integer, String> to Paperless-ngx expected format
- **Date formatting:** @JsonFormat ensures LocalDate serialized as "yyyy-MM-dd" string
- **Builder defaults:** @Builder.Default provides sensible defaults like removeInboxTags = false
- **Immutable design:** @Value with @Builder for thread-safe, predictable document patch requests

**API Integration:**
```java
// PaperlessNgxApiClient interface method
Mono<DocumentResponse> patchDocument(@PathVariable("id") Integer id, 
                                    @RequestBody DocumentPatchRequest request);

// Usage example
var patchRequest = DocumentPatchRequest.builder()
  .title("Updated Title")
  .correspondentId(123)
  .tagIds(List.of(1, 2, 3))
  .customFields(Map.of(5, "extracted value", 6, "another value"))
  .removeInboxTags(true)
  .build();

return apiClient.patchDocument(documentId, patchRequest);
```

**Benefits:**
- **API compatibility:** Custom serialization matches Paperless-ngx expected JSON format
- **Type safety:** Strongly typed fields prevent serialization errors
- **Partial updates:** Only non-null fields included in PATCH requests
- **Immutable operations:** Thread-safe document update operations
- **Testability:** Easy to create test instances with builder pattern

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
