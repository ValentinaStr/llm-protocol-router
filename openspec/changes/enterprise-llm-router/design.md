# Enterprise LLM Router: Design Document

## Context

The Enterprise LLM Router addresses protocol fragmentation across LLM providers. Currently, applications must implement provider-specific logic (OpenAI Chat Completions vs. Response API) inline, leading to duplicated translation code and tight coupling. This design establishes a clean, extensible foundation using Clean Architecture and the Adapter pattern, enabling seamless bidirectional request/response translation.

**Constraints:**
- Must support OpenAI Chat Completions ↔ Response API bidirectional translation
- Must be designed for future expansion (Anthropic, Gemini) without modifying core router logic
- Must adhere to SOLID principles, especially Single Responsibility and Interface Segregation
- Must follow Test-Driven Development (TDD) with unit, smoke, and E2E test coverage
- PoC scope: Functional routing only (exclude advanced performance optimization and security hardening for Phase 1)

## Goals / Non-Goals

**Goals:**
- Enable bidirectional protocol translation between Chat Completions and Response API formats
- Provide a clear adapter framework allowing new providers to be added without modifying core business logic
- Establish Clean Architecture layers (Entities, Use Cases, Interface Adapters) for maintainability
- Demonstrate symmetric mapping (A→B and B→A transformations) with comprehensive test coverage
- Document translation rules and data flow for future provider integrations

**Non-Goals:**
- Advanced performance optimization (caching, connection pooling, batching) — Phase 2+
- Security hardening (auth, encryption, rate limiting) — Phase 2+
- Support for non-OpenAI providers in this PoC (Anthropic, Gemini are future-ready by design, not implemented)
- Advanced logging, tracing, or observability beyond basic error handling
- SDK generation or client library automation

## Decisions

### 1. Clean Architecture with Three-Layer Separation

**Decision: Organize code into Entities → Use Cases → Interface Adapters**

**Rationale:**
- Entities layer: Domain models (`Message`, `Completion`, `CompletionRequest`) represent pure business logic, independent of external frameworks
- Use Cases layer: `ProtocolRouter`, `BidirectionalTranslator` implement routing and translation logic
- Interface Adapters: Protocol-specific adapter classes (`ChatCompletionAdapter`, `ResponseAPIAdapter`) handle API contracts and transformation to/from internal schema

This ensures testability (entities/use cases tested without HTTP), clear responsibility boundaries, and minimal impact when adding providers.

**Alternatives Considered:**
- Layered architecture without explicit domain model separation → rejected (conflates business logic with API details)
- Single monolithic translator class → rejected (violates Single Responsibility, difficult to extend)

---

### 2. Adapter Pattern for Provider Extensibility

**Decision: Use Strategy/Adapter pattern; each provider gets an adapter class inheriting from a base `ProtocolAdapter` interface**

**Rationale:**
- Each protocol (Chat Completions, Response API) is a "strategy" for transforming requests/responses
- Adapters implement `ProtocolAdapter` interface: `validate()`, `request_to_internal()`, `response_to_external()`
- Router uses adapter discovery (registry pattern) to select the right adapter based on incoming request metadata
- Future providers (Anthropic, Gemini) add new adapters without touching router core logic

**Structure:**
```
adapters/
  ├── base.py          # ProtocolAdapter interface
  ├── openai_chat.py   # ChatCompletionAdapter
  └── openai_response.py # ResponseAPIAdapter

router.py
  └── ProtocolRouter.route(request) → selects adapter, invokes transform, returns response
```

**Alternatives Considered:**


#### Data Flow & Adapter Pattern Diagram

```mermaid

### Enterprise LLM Router: Request/Response Flow

<div align="center">

<em>This diagram shows how a client request is routed, adapted, translated, and returned in the Enterprise LLM Router.</em>

```mermaid
flowchart TD
    A["Client Request<br/>(Chat Completion or Response API)"]
    B["1. ProtocolRouter<br/>Discovers Protocol"]
    C["2. Adapter Selected<br/>(ChatCompletionAdapter or ResponseAPIAdapter)"]
    D["3. request_to_internal()<br/>Maps to Internal Schema"]
    E["4. BidirectionalTranslator<br/>Performs Mapping"]
    F["5. response_to_external()<br/>Maps to Protocol Response"]
    G["6. Client Response"]

    A --> B
    B --> C
    C --> D
    D --> E
    E --> F
    F --> G

    subgraph "Adapters"
        C
        F
    end

    subgraph "Core Logic"
        B
        D
        E
    end
```

</div>

### 3. Unified Internal Schema

**Decision: Define internal schema (domain entities) supporting features required by both OpenAI protocols**

**Rationale:**
- Chat Completions operates on discrete messages (role: user/assistant, content)
- Response API operates on threads (message queues, assistant context, run state)
- Internal schema must represent both: a `Conversation` (thread context) containing `Message` objects, with optional assistant metadata
- Both adapters map incoming requests to this unified schema, router performs transformations, adapters map back to target protocol

**Internal Schema Outline:**
```python
class Message:
  role: str          # "user" | "assistant" | "system"
  content: str       # Message text
  timestamp?: timestamp

class InternalCompletion:
  messages: List[Message]
  assistant_id?: str        # For Response API context
  thread_id?: str           # For Response API tracking
  model: str
  temperature?: float
  max_tokens?: int
  ... (other fields common to both)
```

**Alternatives Considered:**
- Separate internal schemas per protocol → rejected (defeats the purpose of translation, increases adapter complexity)
- Extend one protocol's schema → rejected (conflates one provider's design with universal concepts)

---

### 4. Bidirectional Translation with Explicit Rule Documentation

**Decision: Implement `BidirectionalTranslator` with explicit, documented mapping rules for Chat Completions ↔ Response API**

**Rationale:**
- Creates a single source of truth for protocol mapping
- Each mapping rule documented with:
  - Source field → Target field mapping
  - Type transformations (e.g., `thread_id` to implicit conversation context)
  - Default handling for optional fields
- Enables rigorous TDD: tests written first for each rule, then implementation
- Facilitates future provider mappings (Anthropic, Gemini) by establishing the pattern

**Mapping Examples (to TBD in specs):**
- Chat Completions `messages: [{role, content}]` → Internal `Conversation.messages: [Message]`
- Response API `thread_id` → Internal `Conversation.thread_id`
- Response API `assistant_id` → Internal `Conversation.assistant_id`
- (More mapping rules documented in specs and code comments)

**Alternatives Considered:**
- Implicit, code-only mappings → rejected (difficult to verify completeness, prone to edge case bugs)
- Framework-based auto-mapping (like pydantic) → rejected in PoC (possible optimization in Phase 2)

---

### 5. Test-Driven Development Workflow

**Decision: Write failing tests FIRST for both directions (A→B and B→A) before implementing adapters**

**Rationale:**
- Unit tests for each adapter's `request_to_internal()` and `response_to_external()` methods
- Smoke tests verify mock provider endpoint connectivity
- E2E tests simulate real agent flows using both protocols
- Ensures bidirectional symmetry (Chat Comp → Internal → Response API → Internal → Chat Comp returns equivalent data)

**Test Structure:**
```
tests/
  ├── unit/
  │   ├── test_chat_completion_adapter.py    # A→Internal and Internal→A
  │   ├── test_response_api_adapter.py       # B→Internal and Internal→B
  │   └── test_bidirectional_translator.py   # A→B and B→A mappings
  ├── smoke/
  │   └── test_mock_endpoints.py             # Connectivity checks
  └── e2e/
      └── test_agent_workflows.py            # Full agent-router flows
```

**Alternatives Considered:**
- Test-last approach → rejected (violates TDD mandate, higher bug risk)
- Integration tests only (no unit tests) → rejected (insufficient coverage for bidirectional logic)

---

### 6. Minimalist PoC Scope

**Decision: Exclude performance optimization, security hardening, and advanced logging in Phase 1**

**Rationale:**
- PoC goal: Prove bidirectional routing with Clean Architecture works
- Performance (caching, batching, connection pooling) and security (auth, encryption, rate limits) are Phase 2+ concerns
- Basic error handling and logging sufficient to debug issues without over-engineering
- Reduces implementation time and cognitive load for initial design validation

**Alternatives Considered:**
- Include all Phase 2+ features now → rejected (scope creep, delays validation, violates PoC principle)
- Zero error handling → rejected (must be able to debug)

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| **Incomplete internal schema** - Missing fields from either protocol may emerge during implementation | Create schema iteratively during TDD; add fields when tests fail. Document schema decisions in specs. |
| **Tight coupling to OpenAI APIs** - If internal schema mirrors OpenAI too closely, adding Anthropic later becomes harder | Validate internal schema against Anthropic/Gemini v0 APIs during Phase 1 design review (no implementation changes needed). |
| **Adapter complexity grows** - Future adapters (Anthropic, Gemini) may have features unmappable to internal schema | Design `extension_data` field in internal schema to hold provider-specific data; document in specs. |
| **Bidirectional symmetry loss** - Translating Chat Comp → Response API → Chat Comp may lose fields | Implement symmetry tests (send A, convert to B, convert back to A, assert equality). Documented in E2E tests. |
| **PoC-to-production migration friction** - Excluding performance/security now means Phase 2 refactoring | Clear documentation of what's missing (Phase 2+). Design allows adding caching/auth layers without touching adapters. |

## Migration Plan

**Deployment:**
1. Deploy router service (or embed in existing service)
2. Configure OpenAI API keys for Chat Completions and Response API endpoints (mock in PoC)
3. Register adapters in router at startup
4. Expose HTTP endpoints accepting both protocol formats
5. Test bidirectional flows via smoke/E2E tests

**Rollback:**
- If bidirectional routing fails, clients fall back to direct provider calls (no breaking changes in PoC)
- Phase 2: Implement feature flags for gradual traffic migration

## Open Questions

1. **HTTP Endpoint Design**: Should the router expose separate endpoints per input protocol (`POST /chat/completions` and `POST /assistants/threads/messages`), or a single unified endpoint with protocol detection? — *To be decided in specs*

2. **Authentication Strategy for PoC**: Should router pass through client auth tokens, or use service-level credentials? — *Out of scope for PoC (Phase 2); use mock tokens in tests*

3. **Error Mapping**: How should provider-specific errors be represented in internal schema? (e.g., OpenAI 429 rate limit → internal error) — *To be documented in bidirectional-translator spec*

4. **Provider Detection**: Should clients explicitly specify target protocol, or should router infer from request schema? — *To be decided in protocol-router spec*

5. **Async vs. Sync: Should adapters support both sync and async calls, or sync-only for PoC?** — *Sync-only for PoC; async considered Phase 2*

---

**Next Steps**: This design will be followed by specs for each capability listed in the proposal. Each spec will define the detailed interface, data structures, and transformation rules for that capability.
