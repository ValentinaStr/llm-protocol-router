# Adapter Framework Specification

## ADDED Requirements

### Requirement: Base Protocol Adapter Interface
The system SHALL define an abstract base class `ProtocolAdapter` that all protocol implementations inherit from.

The interface MUST include:
- `validate(request: dict) -> bool`: Validates incoming request conforms to adapter's protocol
- `request_to_internal(request: dict) -> Conversation`: Transforms external request to internal Conversation
- `response_to_external(response: CompletionResponse) -> dict`: Transforms internal response back to external format
- `get_protocol_name() -> str`: Returns name of protocol (e.g., "openai-chat", "openai-response-api")
- `get_supported_models() -> List[str]`: Returns list of models this adapter supports

#### Scenario: Define ChatCompletionAdapter extending base
- **WHEN** ChatCompletionAdapter is created inheriting from ProtocolAdapter
- **THEN** all abstract methods must be implemented

#### Scenario: Define ResponseAPIAdapter extending base
- **WHEN** ResponseAPIAdapter is created inheriting from ProtocolAdapter
- **THEN** all abstract methods must be implemented

---

### Requirement: Adapter Registry
The system SHALL implement an `AdapterRegistry` that maintains a mapping of protocol names to adapter instances.

Registry MUST:
- Support `register(protocol_name: str, adapter: ProtocolAdapter)` to add adapters
- Support `get(protocol_name: str) -> ProtocolAdapter` to retrieve adapters
- Support `list_adapters() -> List[str]` to list all registered protocol names
- Raise `ProtocolNotSupportedError` if adapter not found
- Support initialization with default adapters (openai-chat, openai-response-api)

#### Scenario: Register Chat Completions adapter
- **WHEN** startup registers ChatCompletionAdapter with name "openai-chat"
- **THEN** registry returns adapter when requested by name

#### Scenario: Attempt to use unregistered protocol
- **WHEN** router tries to get adapter for unregistered protocol "unsupported-protocol"
- **THEN** registry raises ProtocolNotSupportedError

#### Scenario: List all available adapters
- **WHEN** system queries available adapters
- **THEN** registry returns list: ["openai-chat", "openai-response-api"]

---

### Requirement: Adapter Error Handling Contract
The system SHALL define standard exception hierarchy for adapter operations.

Adapters MUST raise:
- `AdapterValidationError`: When request validation fails (invalid schema, missing required fields)
- `AdapterTransformationError`: When transformation between formats fails (unmappable fields, type conversion)
- `AdapterConfigurationError`: When adapter is misconfigured (missing API key, invalid model)

Each exception MUST include:
- Descriptive message explaining the error
- Reference to problematic field/value if applicable
- Original exception (if from external library) in context

#### Scenario: Invalid request format raises AdapterValidationError
- **WHEN** Chat Completions adapter receives request missing required "model" field
- **THEN** adapter raises AdapterValidationError with message indicating missing field

#### Scenario: Transformation failure raises AdapterTransformationError
- **WHEN** adapter encounters field value that cannot be converted (e.g., invalid enum)
- **THEN** adapter raises AdapterTransformationError with details

#### Scenario: Missing configuration raises AdapterConfigurationError
- **WHEN** adapter is instantiated without required API key
- **THEN** adapter raises AdapterConfigurationError during initialization

---

### Requirement: Adapter Factory Pattern
The system SHALL provide an `AdapterFactory` to instantiate adapters with proper configuration.

Factory MUST:
- Accept configuration dict with protocol-specific settings (API keys, endpoints, model whitelist)
- Instantiate correct adapter class based on protocol identifier
- Validate configuration before returning adapter
- Cache adapter instances to avoid re-instantiation
- Support dependency injection for testing (mock providers, test credentials)

#### Scenario: Factory creates ChatCompletionAdapter with config
- **WHEN** factory receives protocol="openai-chat" and config with API key
- **THEN** factory returns initialized ChatCompletionAdapter ready to use

#### Scenario: Factory caches adapter instance
- **WHEN** factory creates adapter for same protocol twice
- **THEN** second call returns cached instance (same object)

#### Scenario: Factory validates configuration
- **WHEN** factory receives incomplete config (missing API key)
- **THEN** factory raises AdapterConfigurationError before returning adapter

---

### Requirement: Adapter Extensibility for Future Providers
The system SHALL design adapter framework to support non-OpenAI providers without modifying core router.

Framework MUST:
- Use composition/delegation, not inheritance-based extension points
- Keep router protocol-agnostic (work with any ProtocolAdapter)
- Support metadata field in internal schema for provider-specific data
- Document protocol mapping patterns for implementers

#### Scenario: Anthropic adapter can be added in future
- **WHEN** Anthropic adapter is created implementing ProtocolAdapter interface
- **THEN** it can be registered and used without modifying router code

#### Scenario: New provider data preserved in metadata
- **WHEN** adapter encounters provider-specific field not in internal schema
- **THEN** field stored in Conversation/CompletionResponse "metadata" without loss
