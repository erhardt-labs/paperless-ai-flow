# Consolidated Learnings

## Memory Bank Creation & Management

### Memory Bank Structure Pattern
**Core Principle:** Memory bank files follow a hierarchical relationship with clear separation of concerns.

**File Hierarchy:**
1. `projectBrief.md` - Foundation document (requirements, features, success criteria)
2. `productContext.md` - Product rationale (problems solved, user value, market gap)
3. `systemPatterns.md` - Architecture and design patterns
4. `techContext.md` - Technology stack and technical decisions
5. `activeContext.md` - Current work state and decisions
6. `progress.md` - Status tracking and implementation roadmap

**Implementation Guidelines:**
- Each file focuses on its specific purpose without duplicating content
- Content builds upon previous files in the hierarchy
- Maintain actionable, decision-ready information throughout
- Use consistent formatting and section organization

### Documentation as Persistent Knowledge
**Pattern: Memory Bank as Single Source of Truth**
- Memory resets require complete reliance on documented knowledge
- Every architectural decision must include rationale for future reference
- Active context bridges sessions by capturing current work state and open decisions
- Technical context captures not just "what" but "why" for technology choices

**Rationale:** Ensures continuity across sessions and provides complete context for implementation decisions.

## Project Analysis & Documentation

### Structured Project Breakdown Pattern
**Approach:** Systematic decomposition of complex project descriptions into organized documentation components.

**Process:**
1. Extract core requirements and constraints
2. Identify key architectural patterns and decisions
3. Document technology choices with trade-offs
4. Capture current state and next steps
5. Create implementation roadmap with priorities

**Benefits:** Enables comprehensive understanding and provides clear implementation guidance.

### Technical Decision Documentation
**Pattern: Decision Context + Rationale + Trade-offs**
- Document not just the decision but the reasoning behind it
- Capture alternatives considered and why they were rejected
- Include future considerations and potential evolution paths
- Link decisions to specific project constraints or requirements

**Example Applications:**
- Lombok vs Records decision with team consistency rationale
- Provider pattern for vendor lock-in prevention
- Stateless design for horizontal scaling enablement

## API Integration & External Dependencies

### API Documentation Integration Pattern
**Approach:** Create dedicated reference files for critical external APIs that directly impact implementation decisions.

**Structure:**
- **Overview** - API purpose and relevance to project
- **Authentication methods** - with recommendations for project context
- **Core endpoints** - organized by usage priority for the project
- **Integration points** - specific implementation guidance
- **Implementation priorities** - high/medium/low based on project needs

**Benefits:** Centralizes API knowledge, informs architecture decisions, enables accurate implementation planning.

### External API Impact on Architecture
**Pattern: API-Driven Design Decisions**
- API capabilities directly influence idempotency strategy (custom fields for state storage)
- Authentication methods determine service architecture (token-based for headless)
- Bulk operations availability affects performance optimization approaches
- API versioning requirements impact compatibility and error handling design

**Implementation Guidelines:**
- Review API documentation before finalizing architecture patterns
- Update activeContext when API knowledge changes implementation approach
- Document API-specific constraints in technical decision rationale

## Multi-Module Architecture & Reactive Programming

### Multi-Module Maven Pattern
**Core Principle:** Split external integrations into dedicated client modules for better maintainability, testability, and reuse.

**Structure:**
- **Application module:** Business logic, domain services, pipeline orchestration
- **Client modules:** External API integration, response mapping, configuration
- **Dependency flow:** Application depends on clients, never the reverse

**Benefits:**
- Independent testing and development of integration vs business logic
- Clear boundaries prevent coupling between external APIs and domain logic
- Client modules can be versioned and reused across projects
- Compilation boundaries enforce architectural constraints

**Implementation Guidelines:**
- Keep API response entities separate from business domain DTOs using dedicated mappers
- Isolate WebClient configuration and authentication in client modules
- Use proper Maven scope (compile vs runtime) for shared dependencies

### Reactive-First Architecture Pattern
**Core Principle:** Use Mono/Flux throughout the entire stack, not just at HTTP boundaries, for consistent non-blocking I/O.

**Implementation Strategy:**
- **Service Layer:** All methods return Mono<T> or Flux<T> for async operations
- **Data Access:** Use reactive repository patterns and WebClient for all I/O
- **Error Handling:** Leverage reactive error operators (onErrorMap, onErrorResume)
- **Testing:** Use reactor-test StepVerifier for async testing

**Benefits:**
- Consistent programming model across all layers
- Better resource utilization through non-blocking I/O
- Natural backpressure handling for streaming operations
- Improved scalability under high concurrent load

**Key Patterns:**
- **AbstractReactivePagedService:** Reusable pattern for handling paginated API responses with backpressure
- **Reactive Composition:** Chain operations using flatMap, map, and filter operators
- **Error Propagation:** Maintain meaningful error context through reactive chains

### Client Module Separation Pattern
**Pattern: Dedicated External Integration Modules**
- **Purpose:** Isolate external API concerns from business logic
- **Structure:** Complete client with entities, DTOs, mappers, services, and configuration
- **Testing:** Independent test suite with WireMock for external API mocking
- **Configuration:** Self-contained configuration classes and properties

**Rationale:** Enables independent evolution of integration logic without affecting core business functionality. Facilitates testing, maintenance, and potential reuse in other projects.

## Refactoring Strategies

### Reactive Migration Pattern
**Approach:** Systematic conversion from blocking to reactive programming model.

**Migration Steps:**
1. **Identify I/O boundaries** - all network calls, database access, file operations
2. **Convert return types** - methods return Mono<T> instead of T, Flux<T> instead of List<T>
3. **Replace blocking calls** - WebClient instead of RestTemplate, reactive repositories
4. **Update composition** - use flatMap, map instead of imperative control flow
5. **Error handling** - convert try/catch to reactive error operators

**Critical Considerations:**
- Ensure proper error propagation through reactive chains
- Maintain meaningful error messages and context
- Convert all synchronous service calls to reactive composition patterns
- Test reactive flows with StepVerifier and reactor-test
