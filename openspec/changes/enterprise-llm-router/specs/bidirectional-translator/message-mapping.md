## Message Mapping: Chat ↔ Response API

Purpose: define deterministic, per-message mapping rules so the `BidirectionalTranslator` can convert individual messages between OpenAI Chat Completions format and the OpenAI Response/Assistants format.

1) Chat -> Response (per-message)
- Chat message fields: `role` ("system"|"user"|"assistant"), `name` (optional), `content` (string), `metadata` (optional JSON), `timestamp`.
- Response API message representation: messages are stored server-side in a thread; when sending a message body it should be represented following the Response API payload (e.g., `input` / `content` objects, with `role` semantics preserved).

Mapping rules (Chat → Response):
Mapping rules (Chat → Response):
- `role`:
  - Preserve the exact Spring `Message`/role semantics; do not remap or reinterpret beyond preserving the original role value expected by the provider integration layer.
- `name`:
  - Preserve `name` exactly on the message object when present. Do NOT move or rename it.
- `content`:
  - Do NOT mutate `content` under any circumstance; copy verbatim into the target message body. The translator MUST NOT perform token counting or token-based truncation.
- `metadata`:
  - Preserve message-level `metadata` exactly. DO NOT move message metadata into conversation-level storage. If the provider supports per-message metadata/annotations, include it there; otherwise pass it through in a provider-compatible message field. Do not aggregate into `ConversationMetadata`.
- `timestamp`:
  - Preserve as message-level metadata only; provider-managed timestamps may differ.

Example (Chat → Response snippet):
```json
{
  "role": "user",
  "content": "What is the weather?",
  "name": "alice"
}
// → translated into Response API call that creates/uses thread and posts equivalent user input
```

2) Response → Chat (per-message)
- Response API thread messages may contain server-managed metadata (thread_id, assistant_id) and message objects with roles. Translator should flatten thread messages into an ordered list of Chat messages.

Mapping rules (Response → Chat):
Mapping rules (Response → Chat):
- `role`:
  - Map provider roles strictly to the canonical `system` / `user` / `assistant` set. If the provider role cannot be mapped exactly to one of these, the translator MUST throw a `TranslationException` of type `Validation` (do NOT silently map to `assistant`).
- `content`:
  - Extract only supported textual content types. If the provider message contains unsupported content types (binary, proprietary blocks), the translator MUST ignore those parts and, if applicable, record an entry in message `metadata.attachments` with references. Do NOT inline unsupported binary data into `content`.
- `thread_id` / `assistant_id`:
  - Do NOT embed provider-internal fields into message `content` or message-level metadata visible to callers; store them only in `ConversationMetadata` or internal metadata repositories used by the router. Ignore provider-internal fields that are not part of the public message payload.

Example (Response → Chat flatten):
```json
// Thread messages (provider)
[ {"role":"system","content":"You are a helpful assistant."},
  {"role":"user","content":"Hello"},
  {"role":"assistant","content":"Hi! How can I help?"} ]

// → Chat messages list (ordered)
[ {"role":"system","content":"You are a helpful assistant."},
  {"role":"user","content":"Hello"},
  {"role":"assistant","content":"Hi! How can I help?"} ]
```

3) Truncation and long-content policy
- The translator MUST NOT perform token management or token-limited truncation. Token budgeting and enforcement are the responsibility of the caller or the provider integration layer.
- If an application-level policy requires chunking or truncation, that logic should live in a dedicated pre-processing component; the translator MAY provide helpers (e.g., chunking utilities) but MUST NOT implicitly enforce token limits.
- When explicit truncation is applied outside the translator, include `metadata.truncated=true` and `metadata.original_length` on the message.

4) Attachments and non-text items
- Preserve attachment metadata at message level in `metadata.attachments` (`url`, `mime`, `size`, `name`). Do NOT inline binary content into `content`.
- If the target protocol supports uploads, the uploader component (outside the translator) should perform uploads and supply provider references; the translator should only insert those references into message-level metadata when provided.

5) Preservation guarantees
- Translator MUST preserve semantic role and relative ordering of messages.
- Translator SHOULD preserve `name` and non-sensitive `metadata` where feasible.

6) Errors and fallbacks
- For Chat → Response mapping, if a message contains unsupported fields the translator should preserve them in message-level metadata and proceed; do NOT mutate content or manage lifecycle concerns.
- For Response → Chat mapping, if the provider role is unknown or cannot be strictly mapped to `system`/`user`/`assistant`, the translator MUST throw `TranslationException` with type `Validation` and include the problematic role and message identifier.
- For unsupported content types in Response → Chat, ignore the unsupported parts and record references in `metadata.attachments` where applicable; do NOT attempt lossy conversions.

This mapping doc is a living artifact; implementers should add provider-specific annotations where necessary.
