# Chat Completion Adapter Specification

## ADDED Requirements

**NOTE (Java/Spring PoC):** Conversation history is managed by Spring AI `ChatMemory` (in-memory for PoC). Adapters SHOULD transform requests into an in-memory `Conversation` model for the translator and router to use. Only protocol-bridge metadata (conversationId ↔ threadId/assistantId and provider options) is persisted to PostgreSQL via `ConversationMetadata`. Do NOT persist full message history to the database for the PoC.


### Requirement: Chat Completions Request Validation
The system SHALL validate incoming OpenAI Chat Completions API requests against the Chat Completions schema.

Validation MUST check:
- `messages` array is present and non-empty
- Each message in messages array has `role` (user|assistant|system) and `content` (string)
- `model` field is present and non-empty string
- `temperature` if provided is float between 0.0 and 2.0 (optional, default 1.0)
- `max_tokens` if provided is positive integer (optional)
- No unknown required fields are present

#### Scenario: Valid Chat Completions request passes validation
- **WHEN** request contains valid messages, model, and optional parameters
- **THEN** validation succeeds and adapter proceeds to transformation

#### Scenario: Missing messages array fails validation
- **WHEN** request is missing "messages" field
- **THEN** adapter raises AdapterValidationError indicating missing field

#### Scenario: Invalid temperature fails validation
- **WHEN** request contains temperature > 2.0
- **THEN** adapter raises AdapterValidationError with temperature bounds

---

### Requirement: Chat Completions to Internal Schema Transformation
The system SHALL transform valid Chat Completions requests to internal Conversation schema.

Transformation MUST:
- Map request.messages[*] to Conversation.messages as Message objects
- Map request.model to Conversation.model
- Map request.temperature to Conversation.temperature (optional)
- Map request.max_tokens to Conversation.max_tokens (optional)
- Set Conversation.assistant_id and Conversation.thread_id to None (not applicable to Chat Completions)
- Generate unique conversation ID and timestamp
- Store original Chat Completions request in metadata["original_request"] for debugging

#### Scenario: Transform Chat request with basic fields
- **WHEN** adapter receives Chat Completions request: `{messages: [{role: "user", content: "Hi"}], model: "gpt-4"}`
- **THEN** returns Conversation with messages=[Message(role="user", content="Hi")], model="gpt-4"

#### Scenario: Transform Chat request with temperature and max_tokens
- **WHEN** adapter receives request with temperature=0.7, max_tokens=100
- **THEN** returns Conversation with same temperature and max_tokens values

#### Scenario: Handle multiple messages
- **WHEN** adapter receives request with 3 messages (user, assistant, user)
- **THEN** returns Conversation with 3 Message objects in same order

#### Scenario: Store original request metadata
- **WHEN** request is transformed
- **THEN** original request dict accessible in Conversation.metadata["original_request"]

---

### Requirement: Internal Schema to Chat Completions Response Transformation
The system SHALL transform internal CompletionResponse back to Chat Completions API format.

Transformation MUST:
- Map CompletionResponse.choices to response.choices array
- For each choice, create object with message: {role, content}, finish_reason
- Include response ID, model, created timestamp
- Include token usage if available: {prompt_tokens, completion_tokens, total_tokens}
- Format as valid OpenAI Chat Completions API response
- Preserve finish_reason from internal format

#### Scenario: Transform single-choice response
- **WHEN** internal CompletionResponse has one choice with message(role="assistant", content="Response")
- **THEN** response formatted as Chat Completions with choices[0].message.content = "Response"

#### Scenario: Transform multi-choice response
- **WHEN** internal response has 3 choices (n=3 parameter)
- **THEN** response formatted with 3 items in choices array

#### Scenario: Include token usage
- **WHEN** CompletionResponse.usage contains token counts
- **THEN** Chat response includes usage object with prompt_tokens, completion_tokens, total_tokens

#### Scenario: Map finish_reason correctly
- **WHEN** internal finish_reason is "stop" or "max_tokens"
- **THEN** Chat response finish_reason matches exactly

---

### Requirement: Chat Completions Protocol Metadata
The system SHALL track Chat Completions protocol metadata for debugging and auditing.

Metadata tracked MUST include:
- Protocol name: "openai-chat-completions"
- Request timestamp
- Response latency (if tracking integration response time)
- Adapter version
- Any validation warnings (non-blocking issues)

#### Scenario: Protocol name recorded
- **WHEN** Chat request is processed
- **THEN** Conversation.metadata["protocol"] = "openai-chat-completions"

#### Scenario: Request-response latency tracked
- **WHEN** external API response is received
- **THEN** Conversation.metadata["latency_ms"] contains milliseconds

---

### Requirement: Error Handling for Chat Completions
The system SHALL provide clear error messages for Chat Completions issues.

Error handling MUST:
- Distinguish between validation errors (bad request) and transformation errors (internal bug)
- Include field name and expected vs actual value for validation errors
- Include stack trace in metadata for transformation errors
- NOT expose OpenAI API keys or internal paths in error messages

#### Scenario: Invalid model name in error message
- **WHEN** Chat request uses unknown model "gpt-999"
- **THEN** adapter raises AdapterValidationError with message: "Unknown model 'gpt-999'"

#### Scenario: Transformation error includes context
- **WHEN** unexpected field transformation fails
- **THEN** error message includes field name and type information for debugging
