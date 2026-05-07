# Protocol Router Specification

## ADDED Requirements

### Requirement: Request Protocol Detection
The system SHALL detect the incoming request protocol automatically and route to appropriate adapter.

Detection MUST:
- Examine request structure to determine if Chat Completions or Response API format
- Use detection rules: Chat Completions has "messages" array at root; Response API has "thread_id" or "assistant_id"
- Support explicit protocol hint in header `X-LLM-Protocol` for ambiguous cases
- Fallback to Chat Completions if ambiguous and no hint provided
- Return clear error if detection fails

#### Scenario: Detect Chat Completions request
- **WHEN** request contains root-level "messages" array and "model"
- **THEN** router identifies protocol as "openai-chat-completions"

#### Scenario: Detect Response API request
- **WHEN** request contains "thread_id" or "assistant_id" at root level
- **THEN** router identifies protocol as "openai-response-api"

#### Scenario: Explicit protocol hint in header
- **WHEN** request includes header `X-LLM-Protocol: openai-response-api`
- **THEN** router uses specified protocol regardless of request structure

#### Scenario: Ambiguous request without hint defaults to Chat
- **WHEN** request structure is ambiguous and no X-LLM-Protocol header
- **THEN** router defaults to Chat Completions protocol

#### Scenario: Detection failure with clear error
- **WHEN** request structure doesn't match any known protocol
- **THEN** router raises ProtocolDetectionError with suggestions

---

### Requirement: Adapter Selection and Invocation
The system SHALL select and invoke the appropriate adapter based on detected protocol.

Selection MUST:
- Query AdapterRegistry for protocol name
- Invoke adapter's validate() method
- Invoke adapter's request_to_internal() to transform request
- Pass Conversation object to business logic
- Invoke adapter's response_to_external() on result
- Return formatted response to client

#### Scenario: Route to Chat Completions adapter
- **WHEN** Chat Completions request is detected
- **THEN** router selects ChatCompletionAdapter from registry

#### Scenario: Route to Response API adapter
- **WHEN** Response API request is detected
- **THEN** router selects ResponseAPIAdapter from registry

#### Scenario: Call adapter in correct sequence
- **WHEN** request is routed
- **THEN** router calls: validate → request_to_internal → use_case → response_to_external in order

---

### Requirement: Router Error Handling and Recovery
The system SHALL handle errors from adapters gracefully and return appropriate responses.

Error handling MUST:
- Catch AdapterValidationError and return 400 Bad Request with validation details
- Catch AdapterTransformationError and return 500 Internal Server Error
- Catch ProtocolNotSupportedError and return 422 Unprocessable Entity
- Include request ID in error response for tracing
- Log error details (excluding sensitive fields)
- NOT expose internal stack traces to client

#### Scenario: Validation error returns 400
- **WHEN** adapter raises AdapterValidationError
- **THEN** router returns HTTP 400 with JSON error details

#### Scenario: Transformation error returns 500
- **WHEN** adapter raises AdapterTransformationError
- **THEN** router returns HTTP 500 with error message and request ID

#### Scenario: Unsupported protocol returns 422
- **WHEN** protocol detection yields unknown protocol
- **THEN** router returns HTTP 422 with list of supported protocols

#### Scenario: Request ID included in response
- **WHEN** any error occurs
- **THEN** error response includes X-Request-ID header for tracing

---

### Requirement: Adapter Registry Integration
The system SHALL manage adapter registration and lifecycle.

Integration MUST:
- Load default adapters (Chat Completions, Response API) at startup
- Support runtime adapter registration (prepare for Phase 2: Anthropic)
- Validate adapter implements ProtocolAdapter interface
- Maintain single registry instance across router lifecycle
- Support querying available protocols for documentation

#### Scenario: Default adapters loaded at startup
- **WHEN** router initializes
- **THEN** Chat Completions and Response API adapters automatically registered

#### Scenario: Query available protocols
- **WHEN** client calls GET /protocols (or documentation endpoint)
- **THEN** router returns list of supported protocol names and descriptions

---

### Requirement: Bidirectional Transformation Support
The system SHALL enable translating requests from one protocol to another.

Support MUST:
- Transform incoming request to internal schema via source adapter
- Transform internal schema to target protocol via target adapter
- Support explicit protocol translation: "transform Chat request to Response API format"
- Track original and target protocols in metadata
- Validate bidirectional mapping is semantically correct

#### Scenario: Translate Chat Completions to Response API
- **WHEN** client sends Chat request with hint to convert to Response API format
- **THEN** router invokes ChatCompletionAdapter to parse, then Response API adapter to format output

#### Scenario: Round-trip translation preserves essential data
- **WHEN** Chat request → internal → Response API → internal → Chat response
- **THEN** data loss is minimal and acceptable (document what's not preserved)

#### Scenario: Protocol conversion tracked
- **WHEN** translation occurs
- **THEN** response metadata includes source_protocol and target_protocol

---

### Requirement: Extensible Protocol Support (Future-Ready)
The system SHALL be designed to add new protocols without modifying router logic.

Design MUST:
- Keep router independent of specific adapter implementations
- Use AdapterRegistry for all adapter lookups (no hardcoded adapter classes)
- Allow configuration-driven protocol registration
- Support protocol versioning for API evolution

#### Scenario: Adding Anthropic adapter in future
- **WHEN** new AnthropicAdapter is created and registered
- **THEN** router automatically routes requests without code changes

#### Scenario: Configuration defines available protocols
- **WHEN** system starts with config file listing registered protocols
- **THEN** router loads adapters from config, not hardcoded
