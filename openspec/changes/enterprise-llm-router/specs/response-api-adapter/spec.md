# Response API Adapter Specification

## ADDED Requirements

**NOTE (Java/Spring PoC):** Conversation history is managed by Spring AI `ChatMemory` (in-memory for PoC). The `ResponseAPIAdapter` SHOULD transform requests into an in-memory `Conversation` model and write only protocol bridge metadata (e.g., `threadId`, `assistantId`) to `ConversationMetadata` persisted in PostgreSQL. Do NOT persist full message history to the database for the PoC.


### Requirement: Response API Request Validation
The system SHALL validate incoming OpenAI Response API (Assistants/Threads) requests.

Validation MUST check:
- `thread_id` or `assistant_id` is present (identifies conversation/assistant context)
- If creating new thread: `messages` array present with at least one message
- If using existing thread: `message` (single message to add) is present with `role` and `content`
- `model` field present (may be defined on assistant or in request)
- No unknown required fields

#### Scenario: Valid thread message creation
- **WHEN** request has valid thread_id and message {role: "user", content: "..."}
- **THEN** validation succeeds

#### Scenario: Valid new thread with assistant
- **WHEN** request has assistant_id and messages array with initial message
- **THEN** validation succeeds

#### Scenario: Missing thread or assistant context fails
- **WHEN** request lacks both thread_id and assistant_id
- **THEN** adapter raises AdapterValidationError

---

### Requirement: Response API to Internal Schema Transformation
The system SHALL transform Response API requests to internal Conversation schema.

Transformation MUST:
- Map thread_id to Conversation.thread_id
- Map assistant_id to Conversation.assistant_id
- Map incoming message(s) to Conversation.messages as Message objects
- Extract model from request or assistant configuration
- Generate conversation ID if new thread
- Store original Response API request in metadata["original_request"]
- Preserve thread context and assistant metadata

#### Scenario: Transform existing thread message
- **WHEN** adapter receives: `{thread_id: "thread_abc", message: {role: "user", content: "Hi"}}`
- **THEN** returns Conversation with thread_id="thread_abc", messages=[Message(role="user", content="Hi")]

#### Scenario: Transform new thread with assistant
- **WHEN** adapter receives: `{assistant_id: "asst_xyz", messages: [{role: "user", content: "Start"}]}`
- **THEN** returns Conversation with assistant_id="asst_xyz", messages from input array

#### Scenario: Preserve thread and assistant IDs
- **WHEN** transformation is complete
- **THEN** both thread_id and assistant_id retained in Conversation for round-trip mapping

#### Scenario: Extract model from assistant
- **WHEN** request doesn't explicitly include model but references assistant
- **THEN** adapter retrieves model from assistant configuration if available

---

### Requirement: Internal Schema to Response API Transformation
The system SHALL transform internal CompletionResponse back to Response API format.

Transformation MUST:
- Include thread_id from original request (preserved in Conversation)
- Create message object from first CompletionResponse choice
- Include run metadata (created_at, model, status: "completed")
- Format compatible with OpenAI Assistants API response structure
- Preserve assistant_id in response metadata
- Return thread message in standard format

#### Scenario: Transform response back to Response API format
- **WHEN** internal CompletionResponse with choice message is available
- **THEN** transforms to Response API message: {id, role, content, created_at}

#### Scenario: Include thread context
- **WHEN** transformation occurs
- **THEN** response includes thread_id from original Conversation

#### Scenario: Include assistant metadata
- **WHEN** transformation includes assistant
- **THEN** response metadata includes assistant_id and run status

---

### Requirement: Thread and Run State Handling
The system SHALL properly handle Thread and Run state semantics from Response API.

Handling MUST:
- Track thread_id through transformations
- Store run status ("in_progress", "completed", "failed") in metadata
- Preserve assistant_id for response attribution
- Support future run step tracking (prepare for Phase 2)
- Map run completion status to response finish_reason

#### Scenario: Map run completed status to finish_reason
- **WHEN** Response API run.status = "completed"
- **THEN** internal finish_reason = "stop" or "success"

#### Scenario: Thread ID consistency
- **WHEN** request specifies thread_id
- **THEN** same thread_id appears in response and internal Conversation

#### Scenario: Run metadata tracked
- **WHEN** response includes run object with run_id
- **THEN** stored in Conversation.metadata["run_id"] for audit trail

---

### Requirement: Assistant Context Preservation
The system SHALL preserve assistant metadata through transformations.

Metadata preservation MUST:
- Keep assistant_id for response attribution
- Track assistant configuration (model, instructions, tools list) in metadata if provided
- Support assistant-level settings (temperature from assistant, default models)
- Distinguish between request-level and assistant-level parameters

#### Scenario: Assistant model as fallback
- **WHEN** request lacks explicit model but references assistant
- **THEN** assistant's configured model is used as fallback

#### Scenario: Assistant context in response
- **WHEN** completion is attributed to specific assistant
- **THEN** response includes reference to which assistant generated it

---

### Requirement: Error Handling for Response API
The system SHALL provide clear error messages for Response API issues.

Error handling MUST:
- Distinguish between invalid thread_id/assistant_id (not found) and malformed requests
- Include thread/run/assistant IDs in error context for debugging
- Map OpenAI-specific errors (404 thread not found) to adapter errors gracefully
- NOT expose OpenAI API keys or internal paths

#### Scenario: Invalid thread ID
- **WHEN** request references non-existent thread_id
- **THEN** adapter raises AdapterValidationError with message about thread not found

#### Scenario: Transformation error with context
- **WHEN** run state cannot be mapped to internal schema
- **THEN** error includes run_id and run.status for debugging
