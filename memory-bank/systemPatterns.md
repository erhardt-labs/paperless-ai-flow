# System Patterns: Paperless-AI Pipeline

## Architecture Overview

### Headless Microservice Architecture
- **Single-responsibility service** focused solely on document processing
- **Stateless design** with all state maintained in Paperless-ngx
- **Event-driven processing** via polling and pipeline execution
- **Container-native** deployment model

### YAML-Driven Configuration
- **Declarative configuration** defining sources, providers, and processing pipelines
- **Runtime reconfiguration** via file watching or service restart
- **Environment variable injection** for sensitive values (API keys, tokens)
- **Schema validation** ensuring configuration correctness at startup

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
