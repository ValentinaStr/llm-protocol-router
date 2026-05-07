# Internal Schema Specification

## ADDED Requirements

### Requirement: Unified Message Representation
The system SHALL define a unified `Message` entity to represent messages from both Chat Completions and Response API protocols.

Each message MUST contain:
- `role`: String field with values "user", "assistant", or "system"
- `content`: String containing the message text
- `timestamp`: Optional ISO 8601 timestamp
- protocol provenance metadata to track source

#### Scenario: Create message from Chat Completions
- **WHEN** Chat Completions adapter receives `{"role": "user", "content": "Hello"}`
- **THEN** adapter creates Message(role="user", content="Hello")

#### Scenario: Create message from Response API
- **WHEN** Response API adapter receives a message from OpenAI threads
- **THEN** adapter creates Message with same role and content structure

---

### Requirement: Unified Conversation Representation
The system SHALL define a unified `Conversation` entity to represent the session context supporting both Chat Completions and Response API.

Conversation MUST contain:
- `messages`: List[Message] containing all messages in order
- `assistant_id`: Optional string for Response API assistant context
- `thread_id`: Optional string for Response API thread tracking
- `model`: Required string naming the LLM model
- `temperature`: Optional float for sampling temperature
- `max_tokens`: Optional integer for response length limit
- `created_at`: ISO 8601 timestamp of conversation initialization
- `metadata`: Optional dictionary for provider-specific or custom data

#### Scenario: Conversation from Chat Completions request
- **WHEN** router receives Chat Completions request with messages array and model
- **THEN** router creates Conversation with parsed messages and model field

#### Scenario: Conversation from Response API request
- **WHEN** router receives Response API request with thread_id and assistant_id
- **THEN** router creates Conversation with thread_id, assistant_id, and empty messages initially

#### Scenario: Conversation supports optional fields
- **WHEN** source protocol includes optional fields (temperature, max_tokens)
- **THEN** Conversation preserves them in corresponding fields

---

### Requirement: Completion Response Representation
The system SHALL define a unified `CompletionResponse` entity for responses from both protocols.

CompletionResponse MUST contain:
- `id`: Unique identifier for the completion
- `choices`: List of completion choices, each with `message` (Message), `finish_reason` (string)
- `model`: String naming the model that produced response
- `usage`: Optional object with `prompt_tokens`, `completion_tokens`, `total_tokens`
- `created_at`: ISO 8601 timestamp
- `metadata`: Optional dictionary for provider-specific extensions

#### Scenario: Response from Chat Completions
- **WHEN** Chat Completions adapter receives OpenAI Chat response
- **THEN** adapter maps to CompletionResponse with choices containing message and finish_reason

#### Scenario: Response from Response API
- **WHEN** Response API adapter receives thread message/run response
- **THEN** adapter maps to CompletionResponse with assistant message in choices

#### Scenario: Token usage tracking
- **WHEN** source response includes token usage
- **THEN** CompletionResponse preserves token counts in usage field

---

### Requirement: Schema Validation
The system SHALL validate Conversation and CompletionResponse objects for completeness and correctness.

Validation MUST:
- Require `messages` to be a non-empty list for Conversation
- Require `model` field to be a non-empty string
- Validate ISO 8601 timestamps format
- Ensure role values are one of: "user", "assistant", "system"

#### Scenario: Valid conversation passes validation
- **WHEN** Conversation has non-empty messages list, valid model, proper role values
- **THEN** validation succeeds

#### Scenario: Invalid conversation raises error
- **WHEN** Conversation has empty messages or missing model
- **THEN** validation raises `SchemaValidationError` with descriptive message

---

### Requirement: Protocol-Agnostic Data Representation
The system SHALL ensure internal schema is not tightly coupled to any single provider's API.

Internal entities MUST:
- Use generic field names (not OpenAI-specific terminology)
- Support extensibility via metadata fields for provider-specific data
- Remain independent of external API schema versions

#### Scenario: Schema works with Anthropic mock data
- **WHEN** Anthropic adapter (future phase) receives request
- **THEN** Anthropic data can be mapped to same internal schema (verification: no schema changes needed)

#### Scenario: Provider-specific extensions stored safely
- **WHEN** adapter encounters non-standard provider fields
- **THEN** fields stored in metadata without breaking core schema
