# Bidirectional Translator Specification

## ADDED Requirements

### Requirement: Chat Completions to Response API Translation
The system SHALL provide rules and logic to translate Chat Completions format to Response API format.

Translation rules MUST include:
- `Chat.messages[*]` → `ResponseAPI.messages[*]` (direct mapping, one message per message)
- `Chat.model` → `ResponseAPI.assistant.model` or request model field
- `Chat.temperature` → stored with thread/assistant context
- `Chat.max_tokens` → response completion parameters
- No existing thread_id/assistant_id → create new thread context
- Preserve message roles and content exactly
- Generate assistant context if none provided

#### Scenario: Translate simple Chat request
- **WHEN** Chat request contains user message "Hello" with model "gpt-4"
- **THEN** Response API format includes message content "Hello", assistant model "gpt-4"

#### Scenario: Translate Chat with multiple messages
- **WHEN** Chat request has 3 messages (user, assistant, user)
- **THEN** Response API format maintains all 3 messages in thread

#### Scenario: Handle Chat-only parameters
- **WHEN** Chat includes temperature and max_tokens
- **THEN** Response API request includes these parameters in appropriate fields

#### Scenario: Create thread context for new Chat request
- **WHEN** Chat request has no thread_id context
- **THEN** translation generates new thread_id and assigns assistant

---

### Requirement: Response API to Chat Completions Translation
The system SHALL provide rules and logic to translate Response API format to Chat Completions format.

Translation rules MUST include:
- `ResponseAPI.messages[*]` → `Chat.messages[*]` array (flatten thread messages)
- `ResponseAPI.assistant.model` or run model → `Chat.model`
- Thread temperature/max_tokens → `Chat.temperature`, `Chat.max_tokens`
- Preserve message roles and content exactly
- Extract model from assistant or run metadata
- Handle thread history (include all messages from thread)

#### Scenario: Translate Response API to Chat format
- **WHEN** Response API response contains thread message "Response"
- **THEN** Chat format includes message in messages array with role "assistant"

#### Scenario: Flatten thread history into Chat messages
- **WHEN** Response API has 5-message thread history
- **THEN** Chat.messages contains all 5 messages in order

#### Scenario: Extract model from assistant
- **WHEN** Response API response references assistant with configured model
- **THEN** Chat.model field set to that model

---

### Requirement: Protocol Mapping Rules Documentation
The system SHALL document explicit mapping rules for all fields in both protocols.

Documentation MUST include:
- Field-by-field mapping table (Chat ↔ Response API)
- Type conversions required
- Default values for fields not present in source protocol
- Fields that cannot be mapped and why
- Loss of information during translation (if any)
- Examples of each mapping

Mapping Table (high-level):
```
Chat Field                    Response API Field            Notes
------                        ------------------            -----
messages (array)             messages (thread)             Same structure
model (string)               assistant.model               May differ between protocols
temperature (float, 0-2)     temperature parameter         Passed through
max_tokens (int)             max_completion_tokens         Passed through
top_p, frequency_penalty     (not mapped in PoC)          Phase 2+ support
thread_id (from ctx)         thread_id                     None for chat only
assistant_id (from ctx)      assistant_id                  None for chat only
```

#### Scenario: Mapping table reference during implementation
- **WHEN** implementing adapter
- **THEN** developer can reference official mapping table for correctness

#### Scenario: Known limitations documented
- **WHEN** feature exists in one protocol but not other
- **THEN** documentation explains why mapping isn't possible

---

### Requirement: Bidirectional Symmetry Verification
The system SHALL verify that A→B→A transformations preserve essential semantics.

Verification rules:
- Message content MUST round-trip exactly
- Message roles MUST round-trip exactly
- Model field MUST round-trip (or documented as lossy)
- Numerical parameters (temperature, max_tokens) MUST be preserved within tolerance
- Non-standard fields preserved in metadata without loss

#### Scenario: Chat→Response→Chat symmetry
- **WHEN** Chat request is translated to Response API then back to Chat
- **THEN** resulting Chat.messages contains same content and roles

#### Scenario: Response→Chat→Response symmetry
- **WHEN** Response API request is translated to Chat then back to Response API
- **THEN** resulting Response API thread_id and messages are preserved

#### Scenario: Numerical parameter preservation
- **WHEN** Chat temperature=0.7 is translated
- **THEN** round-trip temperature remains 0.7 (within 0.01 tolerance)

#### Scenario: Handle lossy transformations gracefully
- **WHEN** translation encounters non-preservable field
- **THEN** field stored in metadata["lossy_fields"] for audit

---

### Requirement: Edge Case Handling
The system SHALL handle edge cases in bidirectional translation.

Edge cases to handle:
- Empty messages array (error or default)
- Very long conversation history (preservation strategy)
- Special characters in message content
- Null/undefined optional fields
- Nested objects in metadata fields
- Protocol-specific enums/constants

#### Scenario: Empty messages array
- **WHEN** source has no messages
- **THEN** translation raises error or stores null, documented behavior

#### Scenario: Very long history translation
- **WHEN** thread has 100+ messages
- **THEN** all messages preserved (no truncation) unless max_tokens override

#### Scenario: Special characters preserved
- **WHEN** message contains emojis, unicode, special symbols
- **THEN** content preserved exactly in translation

#### Scenario: Undefined optional fields
- **WHEN** source omits optional fields (temperature, max_tokens)
- **THEN** translation omits from target or uses documented defaults

---

### Requirement: Translator Error Handling
The system SHALL report errors in translation clearly with recovery suggestions.

Error handling MUST:
- Distinguish between unmappable vs. malformed input
- Include field name that failed and why
- Suggest workarounds if available
- Preserve input data in error context for debugging

#### Scenario: Unmappable field fails gracefully
- **WHEN** protocol-specific field cannot be mapped
- **THEN** TranslationError raised with message explaining which field and why

#### Scenario: Malformed input clear error
- **WHEN** input field has wrong type or value
- **THEN** TranslationError identifies field and expected type/range
