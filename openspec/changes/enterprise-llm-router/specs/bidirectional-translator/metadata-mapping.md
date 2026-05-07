## Metadata Mapping: Conversation & Message Metadata

Purpose: define how the `BidirectionalTranslator` maps metadata fields between Chat and Response protocols while preserving ownership (message-level vs conversation-level) and avoiding lifecycle/token management responsibilities.

Key principles
- Preserve message-level metadata on the message object; do NOT aggregate or move message metadata into conversation-level storage.
- Conversation-level configuration (model, temperature, max_tokens, stop sequences) may be stored in `ConversationMetadata` for routing and provider-call parameterization.
- Translator MUST NOT manage tokens, truncation, or thread lifecycle; these are the caller/provider concerns.

1) Terminology
- Message-level metadata: per-message annotations (e.g., `metadata.attachments`, `metadata.userLocale`, `metadata.customTags`). These stay attached to each message.
- Conversation-level settings: parameters that apply to the whole request or session (e.g., `model`, `temperature`, `max_tokens`, `top_p`, `stop_sequences`). These may be passed as call options to the provider and recorded in `ConversationMetadata` when needed for protocol bridging.

2) Chat → Response metadata mapping
- Message metadata:
  - Preserve exactly on the target message object. If the provider supports message-level annotations, write `message.annotations = originalMetadata` (provider-specific field). If not, include a provider-compatible metadata container but DO NOT move the fields to `ConversationMetadata`.
- Conversation settings:
  - Map `model`, `temperature`, `max_tokens`, `stop_sequences`, `presence_penalty`, `frequency_penalty` to the provider call options. Do NOT change semantics; pass values through as-is.
  - If a Chat request does not include explicit conversation-level settings, the translator SHOULD consult `ConversationMetadata` (if present) to obtain default provider options.
- Provider-internal fields:
  - Do NOT emit provider-internal fields (thread lifecycle flags, internal IDs) into message-level metadata visible to clients. Store provider-internal identifiers in `ConversationMetadata` only.

Example (Chat → Response):
```json
{
  "messages": [
    { "role": "user", "content": "Hi", "metadata": { "userLocale": "ru-RU" } }
  ],
  "model": "gpt-4o",
  "temperature": 0.7
}
// → translator: sends provider call with options {model: "gpt-4o", temperature:0.7}
//           message-level metadata userLocale preserved on the provider message annotations
```

3) Response → Chat metadata mapping
- Provider call response may include usage, model, and provider-internal IDs. Mapping rules:
  - `model` and `usage` may be included in adapter response-level metadata (top-level response metadata) or stored in `ConversationMetadata` for future calls.
  - Provider-internal IDs (thread_id, assistant_id): store in `ConversationMetadata`; DO NOT surface as message-level content or mutate original message `metadata` fields.
  - If provider returns per-message annotations and they are public, map them to message-level `metadata` preserved verbatim.

Example (Response → Chat):
```json
{
  "thread_id": "t-123",
  "assistant_id": "a-456",
  "messages": [ { "role":"assistant", "content":"OK", "annotations": {"confidence":0.9} } ],
  "usage": { "prompt_tokens": 10, "completion_tokens": 20 }
}
// → translator: message retains annotations in message.metadata; thread_id/assistant_id persisted in ConversationMetadata
```

4) Edge cases and rules
- If both message-level metadata and conversation-level metadata contain the same key, message-level wins for that message (no overwriting of message metadata by conversation defaults).
- Translator MUST NOT synthesize or guess conversation-level settings from message-level metadata unless explicitly configured to do so; prefer explicit values in `ConversationMetadata`.
- Any normalization of metadata keys (e.g., renaming provider fields) MUST be explicit and recorded in mapping documentation; do not perform silent key renames.

5) Security and privacy
- The translator MUST NOT copy sensitive fields (PII, auth tokens) from provider-internal metadata into message-level metadata visible to callers. Sensitive provider fields should remain in internal `ConversationMetadata` with restricted access.

6) Implementation notes
- Provide utility methods:
  - `Map<String,Object> extractMessageMetadata(Message message)`
  - `ProviderOptions buildProviderOptions(ConversationMetadata metadata, Map<String,Object> requestOverrides)`
  - `void persistProviderIds(ConversationMetadataStore store, String conversationId, ProviderIds ids)`
- Keep mapping logic pure (no network calls, no thread creation). The ProtocolRouter or provider adapter handles actual provider interaction and lifecycle.

This mapping doc is a living artifact; add provider-specific sections when integrating a new provider.
