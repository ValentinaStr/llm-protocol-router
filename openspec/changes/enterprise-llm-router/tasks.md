# Enterprise LLM Router: Implementation Tasks

## 1. Project Setup (Java + Spring Boot)

- [ ] 1.1 Initialize Spring Boot project with Maven (spring-boot-starter-web, spring-boot-starter-data-jpa)
 - [x] 1.1 Initialize Spring Boot project with Maven (spring-boot-starter-web, spring-boot-starter-data-jpa)
- [x] 1.2 Add Spring AI OpenAI dependency (spring-ai-openai-spring-boot-starter)
- [x] 1.3 Add PostgreSQL JDBC driver and Testcontainers for testing
- [x] 1.4 Add testing dependencies (JUnit 5, Mockito, AssertJ)
 - [x] 1.5 Configure application.yml with database and OpenAI settings
 - [x] 1.6 Create .gitignore and Maven structure (src/main/java, src/test/java, src/main/resources)

## 2. Domain Entities with JPA (Java Persistence)

NOTE: For the Java + Spring AI PoC conversation history is managed by `ChatMemory` (in-memory by default).
PostgreSQL is used for metadata only (protocol bridge mapping and Phase-2 RAG documents).

- [ ] 2.1 Create `ConversationMetadata` @Entity with fields: `id`, `conversationId`, `threadId`, `assistantId`, `protocol`, `protocolMetadata`, `createdAt`, `updatedAt`
- [x] 2.1 Create `ConversationMetadata` @Entity with fields: `id`, `conversationId`, `threadId`, `assistantId`, `protocol`, `protocolMetadata`, `createdAt`, `updatedAt`
- [x] 2.2 Create `Document` @Entity for Phase-2 RAG with fields: `id`, `content`, `embedding`, `sourceId`, `createdAt`
- [ ] 2.3 Add JPA annotations (@Column, @Table) for metadata and document entities
- [ ] 2.4 Create Spring Data JPA Repositories: `ConversationMetadataRepository`, `DocumentRepository`
- [ ] 2.5 Write unit tests for metadata entity constraints and repository methods
- [ ] 2.6 Write integration tests using Testcontainers PostgreSQL for metadata and RAG documents

## 3. Adapter Framework (Spring Beans & Interfaces)

- [ ] 3.1 Create ProtocolAdapter interface with validate(), requestToInternal(), responseToExternal() methods
- [ ] 3.2 Create custom exception classes (AdapterValidationException, AdapterTransformationException)
- [ ] 3.3 Implement AdapterRegistry @Service with register(), get(), list() methods
- [ ] 3.4 Implement AdapterFactory @Service for adapter instantiation and bean management
- [ ] 3.5 Create GlobalExceptionHandler @RestControllerAdvice for Spring error handling
- [ ] 3.6 Write unit tests for AdapterRegistry and AdapterFactory with Mockito
- [ ] 3.7 Add @Autowired and dependency injection configuration
- [ ] 3.8 Add JavaDoc to all framework classes

## 4. Chat Completions Adapter (Spring Component)

- [ ] 4.1 Create ChatCompletionAdapter @Component implementing ProtocolAdapter
- [ ] 4.2 Inject @Autowired ChatClient from Spring AI

- [x] 4.1 Create ChatCompletionAdapter @Component implementing ProtocolAdapter
- [x] 4.2 Inject @Autowired ChatClient from Spring AI
- [ ] 4.3 Implement validate() method for Chat Completions request schema validation
- [ ] 4.4 Implement requestToInternal() method (Chat request → Conversation entity)
- [ ] 4.5 Implement responseToExternal() method (CompletionResponse → Chat response JSON)
- [ ] 4.6 Add error handling with AdapterValidationException throws
- [ ] 4.7 Add logging with SLF4J @Slf4j annotation
- [ ] 4.8 Write unit tests with @ExtendWith(MockitoExtension.class) and @Mock ChatClient
- [ ] 4.9 Write integration tests for Chat Completions adapter transformation
- [ ] 4.10 Smoke test adapter with mock OpenAI endpoint
- [ ] 4.11 Add protocol detection helper for Chat format detection

## 5. Response API Adapter (Spring Component)

- [ ] 5.1 Create ResponseAPIAdapter @Component implementing ProtocolAdapter
- [ ] 5.2 Inject @Autowired ChatClient from Spring AI
 - [x] 5.1 Create ResponseAPIAdapter @Component implementing ProtocolAdapter
 - [x] 5.2 Inject @Autowired ChatClient from Spring AI
- [ ] 5.3 Implement validate() method for Response API (thread/assistant validation)
- [ ] 5.4 Implement requestToInternal() method (Response API → Conversation entity)
- [ ] 5.5 Implement responseToExternal() method (CompletionResponse → Response API format)
- [ ] 5.6 Add thread_id and assistant_id metadata handling with JSON serialization
- [ ] 5.7 Add error handling with AdapterValidationException/TransformationException
- [ ] 5.8 Add logging with @Slf4j for debugging
- [ ] 5.9 Write unit tests with Mockito mocking ChatClient
- [ ] 5.10 Write integration tests for thread management
- [ ] 5.11 Smoke test Response API adapter with mock endpoint
- [ ] 5.12 Add protocol detection helper for Response API format

## 6. Protocol Router (Spring Service)

- [ ] 6.1 Create ProtocolRouter @Service for main routing logic
- [ ] 6.2 Inject @Autowired AdapterRegistry for adapter selection
- [ ] 6.3 Implement protocol detection logic (Chat vs Response API)
- [ ] 6.4 Implement adapter selection from registry by protocol name
- [ ] 6.5 Implement request validation → transformation → response sequence
- [ ] 6.6 Add HTTP error handling mapping (400, 422, 500)
- [ ] 6.7 Add request ID generation using UUID for tracing
- [ ] 6.8 Implement X-LLM-Protocol header support for protocol hints
- [ ] 6.9 Add @Transactional annotation for database operations
- [ ] 6.10 Add @Slf4j logging for debugging protocol selection
- [ ] 6.11 Write unit tests for protocol detection scenarios
- [ ] 6.12 Write unit tests for adapter selection and invocation
- [ ] 6.13 Write unit tests for error handling (validation, transformation, unsupported)
- [ ] 6.14 Write smoke tests with both protocol types

## 7. Bidirectional Translator (Spring Service)

- [ ] 7.1 Create BidirectionalTranslator @Service class
 - [x] 7.1 Create BidirectionalTranslator @Service class
- [ ] 7.2 Implement Chat→Response API translation logic (messages, model, optional fields)
- [ ] 7.3 Implement Response API→Chat translation logic (flatten thread, preserve context)
- [ ] 7.4 Create mapping rules documentation (field-by-field transformation table)
- [ ] 7.5 Add edge case handling (empty messages, special characters, long content)
- [ ] 7.6 Create TranslationException with clear error messaging
- [ ] 7.7 Add @Slf4j logging for translation debugging
- [ ] 7.8 Write unit tests for Chat→Response→Chat round-trip (A→B→A)
- [ ] 7.9 Write unit tests for Response→Chat→Response round-trip (B→A→B)
- [ ] 7.10 Write unit tests for bidirectional symmetry (message preservation, role preservation)
- [ ] 7.11 Write unit tests for edge cases (empty, special chars, long history)
- [ ] 7.12 Write smoke tests for translation through complete router flow

Detailed subtasks for Bidirectional Translator:

- [ ] 7.2.1 Map messages: implement per-message mapping (role, content, name, metadata)
- [ ] 7.2.2 Map metadata: model, temperature, max_tokens, formats, provider-specific fields
- [ ] 7.2.3 Handle long content & attachments: truncation, streaming fallback, chunking
- [ ] 7.2.4 Preserve system messages and roles across protocols
- [ ] 7.3.1 Implement flatten-thread algorithm: convert Response API thread → ordered Chat messages
- [ ] 7.3.2 Reconstruct thread context when translating Chat→Response (seeding thread prompts)
- [ ] 7.4.1 Produce field-by-field mapping doc with examples (A→B and B→A)
- [ ] 7.5.1 Implement normalization utilities (escape/unescape, unicode handling)
- [ ] 7.6.1 Define TranslationException types (Validation, Mapping, Overflow)
- [ ] 7.8.1 Create unit test fixtures for round-trip scenarios (short/long histories)
- [ ] 7.11.1 Add fuzz tests for special characters and UTF edge cases

## 8. Unit Test Suite (JUnit 5 + Mockito)

- [ ] 8.1 Create test fixtures with @TestFixture/@FixtureFactory for Chat/Response API requests
- [ ] 8.2 Configure @SpringBootTest and @ExtendWith(MockitoExtension.class) annotations
- [ ] 8.3 Write entity tests with Jakarta validation assertions

Concrete unit-test subtasks:

- [ ] 8.1.1 Implement reusable test fixtures: ChatRequestFixtures, ResponseApiFixtures, ConversationFixtures
- [ ] 8.1.2 Add sample payloads for edge cases (empty content, max length, special characters)
- [ ] 8.2.1 Configure Testcontainers PostgreSQL for integration tests and a lightweight in-memory profile
- [ ] 8.2.2 Provide test application.yml for CI with Testcontainers settings
- [ ] 8.3.1 Adapter unit tests: ChatCompletionAdapterTest, ResponseAPIAdapterTest (mock ChatClient)
- [ ] 8.3.2 ProtocolRouter unit tests: protocol detection, adapter selection, error mapping
- [ ] 8.3.3 BidirectionalTranslator unit tests: mapping correctness, symmetry, edge cases
- [ ] 8.4.1 Add integration test: Router end-to-end with Mock OpenAI via WireMock
- [ ] 8.4.2 Add CI test script snippet to run Testcontainers-enabled tests
- [ ] 8.4 Write adapter tests with @Mock ChatClient and @InjectMocks for dependency injection
- [ ] 8.5 Write repository tests with @DataJpaTest for testing Spring Data JPA
- [ ] 8.6 Add @TempDir and temp file fixtures for test isolation
- [ ] 8.7 Configure code coverage with JaCoCo Maven plugin
- [ ] 8.8 Ensure all tests pass with >80% code coverage
- [ ] 8.9 Document test execution: mvn test
- [ ] 8.10 Create test report configuration in pom.xml

## 9. Smoke Test Suite (Integration with Testcontainers)

- [ ] 9.1 Create mock OpenAI Chat Completions provider (WireMock or Spring MockMvc)
- [ ] 9.2 Create mock OpenAI Response API provider (WireMock or Spring MockMvc)
- [ ] 9.3 Implement mock request tracking for assertion verification
- [ ] 9.4 Write smoke tests for Chat Completions adapter with mock provider
- [ ] 9.5 Write smoke tests for Response API adapter with mock provider
- [ ] 9.6 Write smoke tests for router protocol detection and routing
- [ ] 9.7 Write smoke tests for bidirectional translation through router
- [ ] 9.8 Write smoke tests for error condition handling (mock 500, timeouts)
- [ ] 9.9 Use @SpringBootTest with TestRestTemplate for HTTP testing
- [ ] 9.10 Use Testcontainers for PostgreSQL in smoke tests
- [ ] 9.11 Configure smoke test startup/shutdown of mocks
- [ ] 9.12 Document smoke test execution: mvn verify -P smoke-tests

## 10. End-to-End Test Suite (Full Spring Context)

- [ ] 10.1 Create agent fixture classes for simulating real workflows
- [ ] 10.2 Use @SpringBootTest(webEnvironment = RANDOM_PORT) for full context testing
- [ ] 10.3 Use TestRestTemplate to make HTTP calls to running server
- [ ] 10.4 Write E2E tests for Chat Completions single-turn workflow
- [ ] 10.5 Write E2E tests for Chat Completions multi-turn workflow (Conversation persistence)
- [ ] 10.6 Write E2E tests for Chat with temperature/max_tokens parameters
- [ ] 10.7 Write E2E tests for Response API thread creation and messaging
- [ ] 10.8 Write E2E tests for Response API multi-message thread (ordering verification)
- [ ] 10.9 Write E2E tests for Chat→Response API translation with state persistence
- [ ] 10.10 Write E2E tests for Response API→Chat translation with message flattening
- [ ] 10.11 Write E2E tests for error propagation and graceful REST responses
- [ ] 10.12 Write E2E tests for concurrency (2 concurrent, 10 concurrent requests)
- [ ] 10.13 Verify all E2E tests pass under 60 seconds
- [ ] 10.14 Document E2E test execution: mvn verify -P e2e-tests

## 11. REST API Endpoints (Spring Web)

- [ ] 11.1 Create ProtocolRouterController @RestController with @RequestMapping
- [ ] 11.2 Implement POST /api/v1/router/route endpoint with request parsing
- [ ] 11.3 Implement request header parsing for X-LLM-Protocol hint
- [ ] 11.4 Implement response formatting and HTTP status code mapping
- [ ] 11.5 Add request/response logging with @Slf4j without exposing secrets
- [ ] 11.6 Create GET /api/v1/router/status endpoint for health checks
- [ ] 11.7 Create GET /api/v1/router/protocols endpoint to list supported protocols
- [ ] 11.8 Inject ConversationStateStore for persistence operations
- [ ] 11.9 Add @Configuration class for Spring beans (ChatClient, etc.)
- [ ] 11.10 Add @RestControllerAdvice GlobalExceptionHandler for error responses
- [ ] 11.11 Configure application.yml with server.port, spring.datasource, spring.ai settings
- [ ] 11.12 Implement graceful shutdown with @PreDestroy methods

## 12. Documentation

- [ ] 12.1 Create ARCHITECTURE.md with system architecture diagram and component descriptions
- [ ] 12.2 Create DATA_FLOW.md documenting Chat and Response API request flows
- [ ] 12.3 Create BIDIRECTIONAL_MAPPING.md with mapping tables and transformation rules
- [ ] 12.4 Create TDD_WORKFLOW.md explaining JUnit 5 test-driven development process
- [ ] 12.5 Create EXTENSION_GUIDE.md for adding new providers (e.g., Anthropic) in Phase 2
- [ ] 12.6 Create LIMITATIONS.md documenting PoC scope and Phase 2+ work
- [ ] 12.7 Create DEPLOYMENT.md with Maven build, Docker setup, PostgreSQL configuration
- [ ] 12.8 Create DATABASE.md documenting Conversation and Message table schema
- [ ] 12.9 Create API.md documenting REST endpoints (POST /api/v1/router/route, GET /api/v1/router/status)
- [ ] 12.10 Create GLOSSARY.md defining key terms (protocol, adapter, schema, thread_id, etc.)
- [ ] 12.11 Update README.md with quick start (mvn spring-boot:run), main docs links
- [ ] 12.12 Add JavaDoc comments to all public classes and methods

## 13. Testing and Validation (Maven Verification)

- [ ] 13.1 Run unit tests and verify >80% code coverage: mvn test
- [ ] 13.2 Generate JaCoCo coverage report: mvn jacoco:report
- [ ] 13.3 Run smoke tests with Testcontainers: mvn verify -P smoke-tests
- [ ] 13.4 Run E2E tests with full Spring context: mvn verify -P e2e-tests
- [ ] 13.5 Verify bidirectional symmetry: A→B→A and B→A→B round-trips (E2E test cases)
- [ ] 13.6 Test error paths (invalid requests, adapter errors, timeouts)
- [ ] 13.7 Verify request/response tracing with request IDs in logs
- [ ] 13.8 Verify Conversation persistence: Save/retrieve from PostgreSQL
- [ ] 13.9 performance check: Verify request latency acceptable for PoC
- [ ] 13.10 Integration test: Full flow with mock OpenAI + real PostgreSQL

## 14. Final Review and Cleanup (Java/Spring Best Practices)

- [ ] 14.1 Code review: Check SOLID principles (SRP, ISP, DI) with Spring services
- [ ] 14.2 Code review: Verify no hardcoded API keys or sensitive data in code
- [ ] 14.3 Add @Nullable/@NonNull annotations for null safety
- [ ] 14.4 Run Maven static analysis: mvn checkstyle:check, mvn spotbugs:check
- [ ] 14.5 Fix style issues with spotless or Maven Compiler plugin
- [ ] 14.6 Create CONTRIBUTING.md for future developers
- [ ] 14.7 Verify all tests @Transactional for database isolation
- [ ] 14.8 Verify all tests independent and can run in any order
- [ ] 14.9 Create checklist for Phase 2: Async/reactive (Project Reactor), caching (Spring Cache)
- [ ] 14.10 Create checklist for Phase 2: Security (Spring Security), authentication
- [ ] 14.11 Create checklist for Phase 2: Anthropic/Gemini providers as new Spring components
- [ ] 14.12 Final verification: mvn clean verify, all tests pass, coverage >80%, docs complete
