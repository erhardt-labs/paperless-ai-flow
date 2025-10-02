---
Date: 2025-10-02
TaskRef: "Comprehensive Integration Testing for DocumentPollingIntegrationConfig - Queue Management and Locking"

Learnings:
- Spring Integration's DirectChannel processes messages immediately in separate threads, requiring Awaitility for async assertions in tests
- When testing with multi-module Maven projects, must use `./mvnw clean test` from root (NOT `./mvnw test -pl app`) to ensure Lombok annotation processing works correctly
- Awaitility 4.2.0 is essential for testing async Spring Integration flows with proper wait conditions using `await().atMost(Duration).untilAsserted()`
- WireMock URL pattern matching requires `urlMatching("/api/documents/\\d+/.*")` for flexible path matching with trailing slashes
- @SuperBuilder-annotated DTOs don't generate toBuilder() the same way as regular @Builder, causing compilation issues when building with `-pl app` flag
- Testing queue capacity behavior requires understanding that DirectChannels auto-process, making traditional queue-full scenarios challenging to test
- Document locking and unlocking is the critical behavior to verify - queue capacity is a performance optimization
- Mock setup using invocation.getArgument(index, Class.class) is safer than casting to prevent NullPointerExceptions

Difficulties:
- Initial tests failed due to DirectChannel's immediate async processing - documents don't stay queued for verification
- Maven module-specific builds (`-pl app`) caused Lombok annotation processing failures, leading to "method not found" errors
- Complex backpressure tests with `Mono.never()` or `Mono.delay()` caused timing issues and InterruptedExceptions
- WireMock stub patterns needed adjustment from exact URL matching to regex patterns for trailing slash handling

Successes:
- Achieved 100% test pass rate (8/8 tests) for comprehensive integration testing
- Successfully implemented Awaitility for reliable async testing in Spring Integration context
- Created robust error handling tests covering all pipeline stages (OCR, Metadata Extraction, Patching, Saving)
- Verified critical document locking/unlocking behavior across success and failure scenarios
- Added queue capacity backpressure test verifying polling stops when limit reached
- All tests use WireMock for realistic API simulation without external dependencies

Improvements_Identified_For_Consolidation:
- Awaitility Pattern: Use `await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertions)` for async Spring Integration tests
- Maven Multi-Module Testing: Always run tests from project root with `./mvnw clean test` to ensure proper annotation processing
- Spring Integration Testing: DirectChannels process immediately - use Awaitility for assertions, don't assume messages stay in queue
- WireMock URL Patterns: Use `urlMatching()` with regex patterns for flexible URL matching in integration tests
- Document Lock Testing: Focus on lock acquisition and release behavior rather than complex queue state verification
- Safe Mock Argument Extraction: Use `invocation.getArgument(index, Class.class)` instead of casting for null-safety
---

---
Date: 2025-02-10
TaskRef: "Advanced Queue Management Implementation - Document Processing Pipeline"

Learnings:
- DocumentPollingIntegrationConfig now implements intelligent queue capacity management with remainingCapacity checks
- QueueChannel with fixed capacity (25) prevents system overload while providing natural backpressure handling
- Non-blocking send operations using send(message, 0) enable immediate acceptance/rejection feedback
- Document-level locking with IdLockRegistryService prevents race conditions across concurrent processing threads
- Smart lock cleanup pattern: automatically unlock documents when queue is full or processing fails
- take(remainingCapacity) optimization limits documents processed per polling cycle to respect queue limits
- Reactive document querying with Flux-based pagination works seamlessly with queue capacity constraints
- Message headers preserve pipeline context (pipeline definition, pipeline name) throughout processing chain
- Enhanced error handling ensures proper resource cleanup with automatic document unlocking on failures

Difficulties:
- Complex interaction between document locking, queue capacity, and reactive streaming required careful coordination
- Error handling needed to distinguish between queue-full scenarios vs actual processing failures
- Pipeline-specific processing while respecting shared queue capacity limits required thoughtful design

Successes:
- Complete queue management implementation with capacity awareness and backpressure handling
- Automatic resource cleanup preventing document lock leaks in error scenarios
- Optimal throughput with take(remainingCapacity) preventing queue overflow while maximizing processing
- Clean separation between queue management logic and document processing logic
- Comprehensive logging of queue status, capacity decisions, and processing outcomes

Improvements_Identified_For_Consolidation:
- Advanced Queue Management Pattern: Capacity-aware polling with non-blocking operations and automatic cleanup
- Document Lock Management: tryLock/unlock pattern with automatic cleanup on queue rejection or processing failure  
- Spring Integration Enhancement: QueueChannel configuration with capacity limits and DirectChannel immediate processing
- Reactive Backpressure: take() operation integration with queue capacity for optimal throughput control
---
Date: 2025-01-01
TaskRef: "ICEpdf Integration Update - PDF Processing Library Migration"

Learnings:
- Successfully updated PDF processing from PDFbox to ICEpdf for improved image quality and OCR results
- ICEpdf provides superior rendering quality with 300 DPI output and built-in antialiasing
- ICEpdf uses 72 DPI baseline requiring RENDER_SCALE = TARGET_DPI / 72f calculation for proper scaling
- Memory management pattern with try-finally blocks ensures proper resource disposal of ICEpdf Document instances
- PdfOcrService reactive processing using Schedulers.boundedElastic() works seamlessly with ICEpdf integration
- ICEpdf exception handling includes PDFSecurityException for encrypted/password-protected PDFs

Difficulties:
- ICEpdf has different API patterns compared to PDFbox requiring code restructuring in PdfOcrService
- ICEpdf Document resource management requires explicit disposal() calls in finally blocks
- Maven dependency change from org.apache.pdfbox:pdfbox to org.icepdf.os:icepdf-core:6.3.2

Successes:
- Clean migration with improved image quality for OCR processing
- Maintained reactive processing patterns throughout the conversion process
- Memory management improvements with proper resource disposal patterns
- Enhanced rendering quality with antialiasing and high DPI output (300 DPI vs default 72 DPI)

Improvements_Identified_For_Consolidation:
- ICEpdf integration pattern for PDF-to-image conversion with proper resource management
- High-quality rendering configuration (300 DPI, antialiasing) for optimal OCR results
- Error handling patterns specific to ICEpdf (PDFSecurityException for encrypted PDFs)
- Reactive PDF processing with ICEpdf on Schedulers.boundedElastic() thread pool
---
