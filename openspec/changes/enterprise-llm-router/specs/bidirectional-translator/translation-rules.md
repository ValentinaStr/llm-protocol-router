## Translation Rules: Field-by-Field Mapping and Examples

Purpose: provide an authoritative, terse field-by-field mapping for the `BidirectionalTranslator` used by adapters and the `ProtocolRouter`. This document complements `message-mapping.md` and `metadata-mapping.md` and contains concrete examples, error conditions, and test-case ideas.

1. Overview
- Goal: ensure loss-minimal, predictable, auditable translation between Chat Completions-style requests/responses and Response/Assistants-style thread messages.
- Scope: message fields, conversation-level options, attachments, error handling, and integration points with utilities (`ContentChunker`, `AttachmentUtils`).

2. Canonical structures
- Chat message (canonical input/output inside system):
  - `role`: `system` | `user` | `assistant`
  - `name`: optional
  - `content`: string (textual preformat)
  - `metadata`: map (message-level annotations)
  - `timestamp`: ISO string (informational)
- ConversationMetadata (session-level):
  - `conversationId`, `threadId`, `assistantId`, `model`, `temperature`, `max_tokens`, `protocolMetadata`

3. Chat → Response mapping (request side)
- Role: preserve exact value; translator passes `role` into provider message role field. Do NOT remap.
- Name: keep on message object as `name`. If provider supports `author`/`name`, include it. Otherwise include inside message-level `annotations` with key `name`.
- Content: copy verbatim to provider message payload. The translator MUST NOT alter content or perform token-based truncation.
- Message metadata: pass through message-level metadata to provider message `annotations` when supported; otherwise include a provider-compatible metadata container. Do NOT aggregate into conversation-level storage.
- Conversation options: map `model`, `temperature`, `max_tokens`, `stop_sequences` to provider call options without semantic changes. If absent in request, consult `ConversationMetadata`.
- Attachments: leave as metadata placeholders (see `AttachmentUtils`). The translator SHOULD NOT perform uploads; uploader components handle uploads and pass provider references back to translator if needed.

Example:
Chat request:
```
{ "messages": [{"role":"user","content":"Hello","metadata":{"locale":"ru-RU"}}], "model":"gpt-4o" }
```
Provider call (conceptual): includes model=gpt-4o, and the first message with content "Hello" and annotations {locale: "ru-RU"}.

4. Response → Chat mapping (response side)
- Role mapping: provider role must map strictly to `system`/`user`/`assistant`. If mapping impossible -> throw `TranslationException(Type.VALIDATION)`.
- Content extraction: extract textual parts only. If provider returns `parts` array, join with '\n'. Ignore unsupported non-text parts; record references in message `metadata.attachments` if available.
- Provider internal fields: `thread_id`, `assistant_id`, `internal_flags` — DO NOT surface in message-level `metadata` visible to callers. Persist these into `ConversationMetadata` via `ConversationMetadataStore`.
- Usage & model: expose at top-level adapter response metadata (not per-message). Optionally persist in `ConversationMetadata` for defaults.

Example:
Provider response (conceptual):
```
{ "thread_id":"t-1","messages":[{"role":"assistant","content":"OK","annotations":{"confidence":0.9}}],"usage":{...}}
```
Translated Chat response: message content = "OK", message.metadata.annotations = {confidence:0.9}; thread_id persisted separately.

5. Attachments
- Storage: attachments remain in message-level metadata under `metadata.attachments` with fields `{id,url,mime,size,name}`.
- Placeholders: content may contain placeholders like `[attachment:{id}]`. Use `AttachmentUtils.replacePlaceholders()` to substitute provider references when uploader returns them.
- Uploading: translator does not perform uploads. Uploading and provider-specific reference resolution is the responsibility of the adapter or higher-level integration.

6. Chunking and streaming
- Translator does not perform token counting or automatic chunking. If the caller requests chunking, pre-processing should call `ContentChunker` to split `content` into chunks and then invoke provider calls per chunk.
- When chunking is applied by pre-processing, translator helpers MUST preserve message-level metadata and include `metadata.chunk_index` and `metadata.chunks_total` on each chunked message.

7. Error handling
- Validation errors (unknown provider role, unsupported content mapping) -> throw `TranslationException(Type.VALIDATION)` with details.
- Mapping errors (attachment conversion failure, required field missing) -> throw `TranslationException(Type.MAPPING)`.
- Overflow errors (when caller applies explicit truncation incorrectly) -> `TranslationException(Type.OVERFLOW)`.

8. Test coverage suggestions (minimum)
- Unit: Chat→Response mapping for role/name/content/metadata passthrough.
- Unit: Response→Chat strict role mapping (known roles pass, unknown role throws).
- Unit: Attachment extraction and placeholder replacement.
- Integration: Round-trip A→B→A with short history preserving roles and metadata.
- Smoke: Chunked long message flow using `ContentChunker` helpers.

9. Integration notes for implementers
- Keep `BidirectionalTranslator` pure and free of network calls. Use `ConversationMetadataStore` for provider ids and use `AttachmentUtils` and `ContentChunker` for attachments/chunking.
- Document any provider-specific deviations in `specs/bidirectional-translator/provider-<name>.md`.

10. Change log
- v1.0 — initial rules (aligned with ChatMemory-first PoC; message-level metadata preserved; PostgreSQL used for metadata only)
