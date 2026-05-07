# End-to-End Test Suite Specification

## ADDED Requirements

### Requirement: Agent Workflow Simulation - Chat Completions
The system SHALL have E2E tests simulating realistic agent workflows using Chat Completions protocol.

E2E tests MUST:
- Simulate multi-turn conversation with Chat Completions
- Include user messages, assistant responses, system prompts
- Verify conversation state maintains history correctly
- Test temperature and max_tokens parameters work end-to-end
- Simulate agent using router to query multiple turns

Test scenarios:
- **Simple query**: User asks question, agent responds, state persists
- **Multi-turn**: 3+ agent turns with context maintained
- **Parameter variation**: Different temperatures affect response (where mockable)
- **Error recovery**: Agent handles errors gracefully

Each test uses mock provider and verifies:
- All turns succeed
- Message history correct
- Response valid
- Router state consistent

#### Scenario: E2E single-turn Chat interaction
- **WHEN** agent sends user message through router (Chat format)
- **THEN** agent receives assistant response
- **AND** response contains valid message with role "assistant"

#### Scenario: E2E multi-turn Chat conversation
- **WHEN** agent sends 3 consecutive user messages
- **AND** each turn adds assistant response
- **THEN** final history contains all 6 messages in correct order
- **AND** context maintained throughout

#### Scenario: E2E Chat with parameters
- **WHEN** agent sends request with temperature=0.2, max_tokens=100
- **THEN** router passes parameters through
- **AND** response respects max_tokens limit (mocked)

---

### Requirement: Agent Workflow Simulation - Response API
The system SHALL have E2E tests simulating realistic agent workflows using Response API (Assistants/Threads).

E2E tests MUST:
- Simulate agent using thread/assistant pattern
- Create thread, add messages, retrieve responses
- Verify thread context persists across requests
- Test assistant metadata handling
- Verify thread history accumulates

Test scenarios:
- **Thread creation**: Create new thread with initial message
- **Message addition**: Add message to existing thread
- **Multi-turn thread**: 3+ messages in thread with context preserved
- **Assistant switching**: Change assistant context (if applicable)

#### Scenario: E2E Response API thread creation and message
- **WHEN** agent creates new thread with initial user message
- **THEN** agent receives thread_id
- **AND** response includes created thread message

#### Scenario: E2E Response API multi-message thread
- **WHEN** agent adds message 1, receives response, adds message 2, receives response
- **THEN** thread_id remains same across interactions
- **AND** thread history contains all messages

#### Scenario: E2E Response API thread context persistence
- **WHEN** agent fetches existing thread and adds new message
- **THEN** new message added successfully
- **AND** thread includes all previous messages in history

---

### Requirement: Cross-Protocol Translation E2E Tests
The system SHALL have E2E tests verifying bidirectional protocol translation in realistic scenarios.

E2E tests MUST:
- Start with Chat agent, translate to Response API, verify Response API tools work
- Start with Response API agent, translate to Chat, verify Chat tools work
- Verify translation doesn't lose critical information
- Simulate agent transparently switching protocols

Test scenarios:
- **Chat→Response**: Agent sends Chat request, router translates to Response API
- **Response→Chat**: Agent sends Response API request, router translates to Chat
- **Round-trip**: Chat→Response→Chat with verification
- **Data preservation**: Message content, roles, model all preserved

#### Scenario: E2E Chat→Response API translation
- **WHEN** agent sends Chat Completions format request with messages
- **THEN** router detects Chat format and translates to Response API
- **AND** Response API-compatible response returned
- **AND** message content preserved exactly

#### Scenario: E2E Response API→Chat translation
- **WHEN** agent sends Response API format request with thread_id
- **THEN** router detects Response API format and translates to Chat
- **AND** Chat Completions format response returned
- **AND** thread context preserved in response metadata

#### Scenario: E2E round-trip translation preserves data
- **WHEN** agent sends Chat request, router translates to Response API and back to Chat
- **THEN** final response message content matches input
- **AND** roles and model preserved
- **AND** acceptable data loss documented (if any)

---

### Requirement: Error Handling E2E Tests
The system SHALL verify error handling across agent-to-router workflows.

E2E tests MUST cover:
- Invalid request handling (agent sends malformed request)
- Adapter error propagation (downstream error)
- Timeout handling (slow response)
- Protocol mismatch (agent sends wrong protocol)
- Graceful degradation

Each test verifies:
- Error returned to agent with meaningful message
- Request ID included for tracing
- No partial state left (transaction semantics)
- Retryable vs. permanent errors distinguished

#### Scenario: E2E invalid request error handling
- **WHEN** agent sends request missing required field
- **THEN** router returns 400 Bad Request
- **AND** error message indicates which field is missing
- **AND** request ID included in response

#### Scenario: E2E upstream error propagation
- **WHEN** mock provider returns 500 error
- **THEN** router catches and returns 500 to agent
- **AND** error message indicates issue upstream
- **AND** doesn't expose internal stack trace

#### Scenario: E2E timeout handling
- **WHEN** mock provider is slow (timeout scenario)
- **THEN** router returns timeout error
- **AND** error indicates endpoint unresponsive
- **AND** request ID included for investigation

#### Scenario: E2E protocol mismatch detection
- **WHEN** agent sends ambiguous request and no protocol hint
- **THEN** router applies default (Chat Completions)
- **OR** router returns error if detection fails

---

### Requirement: Concurrency E2E Tests
The system SHALL verify router handles concurrent requests correctly.

E2E tests MUST:
- Simulate multiple agents sending requests simultaneously
- Verify responses matched to correct requests
- Ensure no request/response mixing
- Test thread-safety of adapters and router

Test scenarios:
- **2 concurrent Chat requests**: Different agents both sending Chat
- **Mixed protocols**: One Chat, one Response API simultaneously
- **10 concurrent requests**: Stress test with many agents
- **Request ordering**: Verify FIFO or documented order

#### Scenario: E2E two concurrent Chat requests
- **WHEN** agent A and agent B both send Chat requests simultaneously
- **THEN** agent A receives agent A's response
- **AND** agent B receives agent B's response
- **AND** no mixing or corruption of responses

#### Scenario: E2E mixed protocol concurrent requests
- **WHEN** agent A sends Chat, agent B sends Response API simultaneously
- **THEN** both route correctly to their adapters
- **AND** responses returned to correct agents

#### Scenario: E2E stress test with concurrent requests
- **WHEN** 10 agents send requests concurrently
- **THEN** all complete successfully
- **AND** all responses valid and unmixed
- **AND** completion time < documented threshold

---

### Requirement: E2E Test Organization and Execution
The system SHALL organize E2E tests with clear structure and reliable execution.

Organization MUST be:
```
tests/e2e/
  ├── test_chat_completions_workflows.py
  ├── test_response_api_workflows.py
  ├── test_translation_workflows.py
  ├── test_error_handling.py
  ├── test_concurrency.py
  └── fixtures.py (mock agents, providers)
```

Execution MUST:
- Be runnable with test runner (pytest)
- Support running full suite or individual test files
- Clean up state between tests (no test pollution)
- Execute in under 60 seconds for rapid feedback
- Support verbose logging for debugging
- Use deterministic mock responses (reproducible)

#### Scenario: Run all E2E tests
- **WHEN** pytest runs tests/e2e/ directory
- **THEN** all E2E tests pass
- **AND** execution completes in under 60 seconds
- **AND** no test pollution (each test independent)

#### Scenario: Run specific E2E test file
- **WHEN** pytest runs tests/e2e/test_chat_completions_workflows.py
- **THEN** only Chat Completions workflow tests execute
- **AND** other tests skipped
