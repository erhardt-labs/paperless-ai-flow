---
Date: 2025-09-21
TaskRef: "Implement parallel AI metadata extraction pipeline step"

Learnings:
- Reactor `Mono.zip()` requires ALL components to emit a value - if any component emits empty, the entire zip operation completes empty
- When handling optional values in reactive streams that need to participate in zip operations, use sentinel values instead of null/empty
- Project Reactor's zip operation is strict - all monos must emit for zip to emit
- `Mono.just((Long) null)` throws NullPointerException, `Mono.fromCallable(() -> null)` can still cause zip completion issues
- Sentinel value pattern works reliably: emit non-null sentinel (e.g., -1L), convert back to null in final mapping
- Spring Integration channels work well for pipeline orchestration with proper error handling
- WireMock integration tests provide excellent coverage for external API interactions without network dependencies
- Java 21's `var` keyword with Lombok builders creates clean, readable code while maintaining type safety
- Parallel AI model execution significantly improves performance over sequential processing

Difficulties:
- Initial confusion about Reactor's zip semantics with empty monos led to failing tests
- Multiple attempts to emit null values in reactive streams failed due to Reactor's strict null handling
- Need to carefully handle null values vs empty streams in reactive pipelines
- Testing reactive code requires understanding of StepVerifier patterns

Successes:
- Successfully implemented parallel AI processing using Reactor's zip operation with sentinel values
- Created comprehensive test coverage including WireMock integration tests for new API endpoints
- Proper Spring Integration channel-based pipeline architecture with metadata extraction step
- Clean separation between AI models, service orchestration, and integration flow
- All four AI extraction models working in parallel: title, tags, correspondent, custom fields
- Robust error handling with fallback values for each extraction type

Improvements_Identified_For_Consolidation:
- Use sentinel values for optional data in Mono.zip operations, convert back to null in final mapping
- Reactor testing patterns with proper null value handling using StepVerifier
- Spring Integration channel-based pipeline patterns for document processing
- Parallel AI processing patterns using Mono.zip for performance optimization
---
