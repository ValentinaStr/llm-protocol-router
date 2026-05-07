# Smoke Test Suite Specification

## ADDED Requirements

### Requirement: Mock Provider Endpoint Setup
The system SHALL provide mock OpenAI provider endpoints for smoke testing.

Mock endpoints MUST:
- Simulate Chat Completions API endpoint (POST /chat/completions)
- Simulate Response API endpoint (POST /threads and POST /threads/{id}/messages)
- Accept valid requests without authentication
- Return valid responses matching OpenAI schema
- Support tracking of received requests for assertion
- Be easily startable/stoppable for test cycles

Mock providers SHOULD:
- Run on localhost with configurable ports
- Return responses in < 100ms for deterministic testing
- Log all requests for debugging
- Support fixture injection to modify responses (success, error scenarios)

#### Scenario: Chat Completions mock endpoint accessible
- **WHEN** smoke test starts mock provider
- **THEN** POST to http://localhost:5001/chat/completions returns 200 with valid response

#### Scenario: Response API mock endpoint accessible
- **WHEN** smoke test starts mock provider
- **THEN** POST to http://localhost:5001/threads/{id}/messages returns 200 with valid response

#### Scenario: Mock tracks requests for assertion
- **WHEN** test sends requests to mock
- **THEN** test can query mock for received requests and verify content

---

### Requirement: Chat Completions Adapter Smoke Test
The system SHALL verify ChatCompletionAdapter connectivity to mock Chat endpoint.

Smoke test MUST:
- Start mock provider
- Create ChatCompletionAdapter with mock endpoint configured
- Send valid Chat Completions request
- Verify response is returned and valid
- Verify request was received by mock
- Verify response format matches Chat Completions schema

#### Scenario: Smoke test Chat Completions round-trip
- **WHEN** smoke test sends valid request to ChatCompletionAdapter pointing to mock
- **THEN** test receives valid Chat Completions response and mock confirms request received

#### Scenario: Smoke test detects mock unavailable
- **WHEN** adapter is configured with unreachable mock endpoint
- **THEN** adapter raises connection error
- **AND** error clearly indicates endpoint not reachable

---

### Requirement: Response API Adapter Smoke Test
The system SHALL verify ResponseAPIAdapter connectivity to mock Response API endpoint.

Smoke test MUST:
- Start mock provider
- Create ResponseAPIAdapter with mock endpoint configured
- Send valid Response API thread message request
- Verify response is returned and valid
- Verify request was received by mock
- Verify response format matches Response API schema

#### Scenario: Smoke test Response API round-trip
- **WHEN** smoke test sends valid thread message request to ResponseAPIAdapter
- **THEN** test receives valid Response API response and mock confirms request received

#### Scenario: Smoke test Response API with new thread
- **WHEN** smoke test sends new thread request with messages
- **THEN** test creates thread successfully and receives valid response

---

### Requirement: Router Integration Smoke Test
The system SHALL verify protocol detection and routing to correct adapter.

Smoke test MUST:
- Start mock provider
- Initialize router with both adapters pointing to mock endpoints
- Send Chat Completions request through router
- Send Response API request through router
- Verify each request routed to correct adapter
- Verify responses returned with correct format

#### Scenario: Router routes Chat request to Chat adapter
- **WHEN** smoke test sends Chat Completions format request to router
- **THEN** router detects protocol and invokes ChatCompletionAdapter
- **AND** response is in Chat Completions format

#### Scenario: Router routes Response API request to Response adapter
- **WHEN** smoke test sends Response API format request to router
- **THEN** router detects protocol and invokes ResponseAPIAdapter
- **AND** response is in Response API format

---

### Requirement: Bidirectional Translation Smoke Test
The system SHALL verify bidirectional translation works end-to-end through router.

Smoke test MUST:
- Initialize router with translation support
- Send Chat request, translate to Response API, verify result valid
- Send Response API request, translate to Chat, verify result valid
- Verify data round-trips correctly
- Verify no exceptions during translation

#### Scenario: Smoke test Chat→Response API translation
- **WHEN** router receives Chat request with translation flag
- **THEN** response is in Response API format with messages preserved

#### Scenario: Smoke test Response API→Chat translation
- **WHEN** router receives Response API request with Chat translation flag
- **THEN** response is in Chat Completions format with messages preserved

---

### Requirement: Error Condition Smoke Tests
The system SHALL verify graceful handling of error conditions.

Smoke tests MUST cover:
- Mock endpoint returning error (500)
- Mock endpoint timing out
- Invalid request format
- Adapter configuration errors (missing API key, invalid model)

Each test verifies:
- Error is caught
- Meaningful error message returned
- No unhandled exceptions escape router
- Error can be logged without crashing

#### Scenario: Smoke test handles mock error response
- **WHEN** mock endpoint returns 500 error
- **THEN** adapter catches error and returns meaningful error response

#### Scenario: Smoke test handles timeout
- **WHEN** mock endpoint doesn't respond within timeout
- **THEN** router raises timeout error with context information

#### Scenario: Smoke test handles invalid request
- **WHEN** invalid request passed to router
- **THEN** router returns 400 error without calling adapters

---

### Requirement: Smoke Test Execution and Reporting
The system SHALL organize smoke tests for reliable execution.

Execution MUST:
- Start mock provider before tests
- Stop mock provider after tests (cleanup)
- Execute in under 30 seconds for rapid feedback
- Report which endpoints were tested
- Report any connectivity issues clearly
- Support running individually or as suite

Reporting MUST:
- Log mock provider startup/shutdown
- Log all requests to mock
- Log adapter responses
- Report coverage (Chat endpoint tested, Response API endpoint tested)

#### Scenario: Run all smoke tests
- **WHEN** pytest runs tests/smoke/ directory
- **WHEN** mock provider is started and stopped cleanly
- **THEN** all smoke tests pass and report endpoints tested

#### Scenario: Smoke tests indicate connectivity status
- **WHEN** smoke tests complete
- **THEN** report shows which endpoints were reachable
- **AND** any connectivity issues clearly reported
