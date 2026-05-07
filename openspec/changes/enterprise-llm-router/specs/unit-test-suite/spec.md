# Unit Test Suite Specification

## ADDED Requirements

### Requirement: Chat Completions Adapter Unit Tests
The system SHALL have comprehensive unit tests for ChatCompletionAdapter in both transformation directions.

Tests MUST cover:
- **request_to_internal transformation**:
  - Valid Chat request → Conversation
  - Missing required fields (messages, model)
  - Invalid message roles
  - Optional parameters (temperature, max_tokens)
  - Boundary values for temperature (0.0, 1.0, 2.0)
  - Large message content (>4096 tokens simulated)
- **response_to_external transformation**:
  - Internal CompletionResponse → Chat response format
  - Single and multiple choices
  - Token usage inclusion
  - Finish reason mapping
- **Error cases**:
  - Invalid request structure
  - Type validation failures
  - Transformation exceptions

Each test MUST:
- Be independent (no test order dependency)
- Include setup/teardown for any mocking
- Assert expected behavior explicitly
- Include comments explaining the test case goal

#### Scenario: Unit test validates request_to_internal happy path
- **WHEN** unit test runs with valid Chat request fixture
- **THEN** test verifies Conversation has correct messages, model, and optional fields

#### Scenario: Unit test validates response_to_external happy path
- **WHEN** unit test runs with internal CompletionResponse
- **THEN** test verifies output matches Chat Completions schema

#### Scenario: Unit test catches validation errors
- **WHEN** unit test provides invalid Chat request (e.g., missing model)
- **THEN** test verifies AdapterValidationError is raised

---

### Requirement: Response API Adapter Unit Tests
The system SHALL have comprehensive unit tests for ResponseAPIAdapter in both directions.

Tests MUST cover:
- **request_to_internal transformation**:
  - Valid thread message request → Conversation
  - Valid new thread request → Conversation
  - Missing thread_id and assistant_id (error)
  - Thread vs. new thread scenarios
  - Assistant context preservation
- **response_to_external transformation**:
  - Internal CompletionResponse → Response API format
  - Thread and run metadata preservation
  - Assistant attribution
- **Error cases**:
  - Invalid thread/assistant IDs
  - Malformed run state
  - Missing required fields

#### Scenario: Unit test validates thread message transformation
- **WHEN** unit test provides existing thread request
- **THEN** test verifies Conversation includes thread_id and messages

#### Scenario: Unit test validates new thread transformation
- **WHEN** unit test provides new thread request with assistant_id
- **THEN** test verifies Conversation initialized with assistant context

---

### Requirement: Bidirectional Translator Unit Tests
The system SHALL have unit tests validating A→B→A transformations.

Tests MUST cover:
- **Chat → Internal → Response API**:
  - Message content preservation
  - Role preservation
  - Model preservation
  - Symmetry verification
- **Response API → Internal → Chat**:
  - Thread message preservation
  - Assistant context handling
  - Message order preservation
- **Edge cases**:
  - Empty messages
  - Single message
  - Long message content
  - Special characters

Each test MUST verify:
- Source and result are semantically equivalent
- No unexpected data loss
- Roles, content, models match

#### Scenario: Unit test validates Chat→Response→Chat round-trip
- **WHEN** test translates Chat to Response API format and back
- **THEN** test asserts messages and roles match original

#### Scenario: Unit test validates Response→Chat→Response round-trip
- **WHEN** test translates Response API to Chat format and back
- **THEN** test asserts thread_id and messages preserved

---

### Requirement: Internal Schema Validation Unit Tests
The system SHALL have unit tests for Conversation and CompletionResponse validation.

Tests MUST cover:
- **Conversation validation**:
  - Non-empty messages list required
  - Valid model field required
  - Valid roles ("user", "assistant", "system")
  - ISO 8601 timestamps valid
  - Optional fields (thread_id, assistant_id) when present
- **CompletionResponse validation**:
  - Non-empty choices required
  - Valid finish_reason values
  - Token usage counts non-negative

#### Scenario: Unit test validates valid Conversation
- **WHEN** test creates valid Conversation object
- **THEN** validation succeeds, no errors raised

#### Scenario: Unit test rejects invalid Conversation
- **WHEN** test creates Conversation with empty messages
- **THEN** validation raises SchemaValidationError

---

### Requirement: Adapter Registry Unit Tests
The system SHALL have unit tests for AdapterRegistry functionality.

Tests MUST cover:
- Register adapter by protocol name
- Retrieve registered adapter
- List all registered adapters
- Error on unregistered protocol
- Initialization with default adapters

#### Scenario: Unit test registers and retrieves adapter
- **WHEN** test registers ChatCompletionAdapter
- **THEN** test retrieves adapter by name and verifies it's the same instance

#### Scenario: Unit test errors on missing protocol
- **WHEN** test attempts to get unregistered protocol
- **THEN** test asserts ProtocolNotSupportedError raised

---

### Requirement: Unit Test Organization and Execution
The system SHALL organize unit tests in standard structure with clear naming.

Structure MUST be:
```
tests/unit/
  ├── test_chat_completion_adapter.py
  ├── test_response_api_adapter.py
  ├── test_bidirectional_translator.py
  ├── test_internal_schema.py
  ├── test_adapter_registry.py
  └── fixtures.py (shared test data)
```

Execution MUST:
- Be runnable with standard test runner (pytest for Python)
- Support filtering by adapter/module
- Report coverage metrics
- Execute in under 10 seconds for all unit tests

#### Scenario: Run all unit tests
- **WHEN** pytest runs tests/ directory
- **THEN** all unit tests pass and coverage reported

#### Scenario: Run adapter-specific tests
- **WHEN** pytest runs tests/unit/test_chat_completion_adapter.py
- **THEN** only Chat Completions adapter tests execute
