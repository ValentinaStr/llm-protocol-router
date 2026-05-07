# Java + Spring AI Implementation Specification

## ADDED Requirements

### Requirement: Spring Boot Project Structure and Dependencies
The system SHALL be implemented using Spring Boot framework with Spring AI for LLM integration and ChatMemory for conversation history.

Project structure MUST include:
- Maven or Gradle build system
- Spring Boot 3.2+ application
- Spring AI OpenAI starter dependency with ChatMemory support
- Spring Data JPA for RAG document persistence (Phase 2+)
- PostgreSQL JDBC driver for RAG vector store
- JUnit 5 for unit testing
- Testcontainers for integration testing
- Lombok for reducing boilerplate
- Jackson for JSON serialization

Maven dependencies:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-core</artifactId>
</dependency>
<!-- RAG support (Phase 2+) -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
</dependency>
```

#### Scenario: Spring Boot project initializes
- **WHEN** application starts with correct configuration
- **THEN** Spring Boot context loads successfully
- **AND** database connection established to PostgreSQL

#### Scenario: Spring AI integration available
- **WHEN** application context initialized
- **THEN** ChatClient bean available for dependency injection
- **AND** OpenAI API client configured from application.yml

---

### Requirement: Spring AI ChatMemory for Conversation History
The system SHALL use Spring AI ChatMemory to persist conversation history automatically.

ChatMemory implementation MUST:
- Store conversation history per conversation_id (unique identifier)
- Automatically append user messages and assistant responses
- Be injectable as @Autowired ChatMemory in services
- Support InMemoryChatMemory for PoC
- Be replaceable with PostgreSQL-backed implementation for Phase 2+
- Retrieve full conversation history on each subsequent request

ChatMemory configuration:
```java
@Configuration
public class ChatMemoryConfig {
    
    @Bean
    public ChatMemory chatMemory() {
        // PoC: Use in-memory storage
        // Phase 2+: Replace with PostgreSQLChatMemory
        return new InMemoryChatMemory();
    }
}
```

Usage in ProtocolRouter:
```java
@Service
public class ProtocolRouter {
    
    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private ChatMemory chatMemory;
    
    public Map<String, Object> route(String conversationId, Map<String, Object> request) {
        // 1. Transform request to Conversation
        Conversation conv = adapter.requestToInternal(request);
        
        // 2. Spring AI ChatMemory automatically retrieves previous messages
        // 3. ChatClient sends request to OpenAI with full history
        ChatResponse response = chatClient.call(
            new Prompt(conv.getMessages()),
            conversationId  // ← ChatMemory key
        );
        
        // 4. Spring AI ChatMemory automatically stores request and response
        // Full history now available for next request
        
        return adapter.responseToExternal(response);
    }
}
```

#### Scenario: ChatMemory stores and retrieves conversation
- **WHEN** first request comes for conversation_id="user123"
- **THEN** ChatMemory stores message in memory with key "user123"

#### Scenario: ChatMemory includes history in next request
- **WHEN** second request comes for same conversation_id="user123"
- **THEN** ChatMemory automatically includes previous messages in prompt to OpenAI

#### Scenario: Multiple conversations isolated
- **WHEN** two requests with different conversation_ids="user123" and "user456"
- **THEN** each has separate history in ChatMemory
- **AND** no cross-contamination between conversations


The system SHALL use Java POJOs with JPA annotations for metadata-only entity mapping.

**CRITICAL ARCHITECTURE NOTE:** Conversation history is stored in Spring AI ChatMemory (automatic, per conversation_id), NOT in PostgreSQL entities. The Conversation and Message entities described in core specs are NOT persisted to database in this implementation. PostgreSQL stores ONLY metadata for protocol bridging.

Entity classes MUST:
- Be annotated with @Entity for JPA persistence (metadata entities only)
- Use @Table for explicit table naming
- Include @Id and @GeneratedValue for primary keys
- Use @Column for explicit field mappings
- Include @Temporal or LocalDateTime for timestamps
- Implement Serializable interface for serialization
- Use Lombok @Data, @NoArgsConstructor, @AllArgsConstructor for reducing boilerplate

ConversationMetadata entity (stores protocol bridge mappings ONLY):
```java
@Entity
@Table(name = "conversation_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, unique = true)
    private String conversationId; // ChatMemory key (Chat Completions protocol)
    
    @Column
    private String threadId; // Response API thread ID (Assistants protocol)
    
    @Column
    private String assistantId; // Response API assistant ID (Assistants protocol)
    
    @Column(nullable = false)
    private String protocol; // "chat-completions" or "assistants"
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(columnDefinition = "jsonb")
    private String protocolMetadata; // JSON with protocol-specific data (model, temperature, max_tokens)
}
```

Document entity (Phase 2+ RAG support, currently empty):
```java
@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String content; // Document text
    
    @Column(columnDefinition = "vector")
    private float[] embedding; // pgvector embedding (Phase 2+)
    
    @Column
    private String sourceId; // Reference to external document source
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

#### Scenario: ConversationMetadata persists protocol mapping to PostgreSQL
- **WHEN** ProtocolRouter receives Chat Completions request with new conversation_id
- **THEN** ConversationMetadata record created with conversationId (ChatMemory key) and protocol="chat-completions"
- **AND** if request translates to Response API call, threadId/assistantId populated from API response
- **AND** protocolMetadata stores model, temperature, max_tokens as JSON

#### Scenario: ProtocolRouter retrieves metadata for protocol translation
- **WHEN** request arrives with conversationId or threadId
- **THEN** ConversationMetadata looked up to map between protocols
- **AND** Chat Completions → Response API uses threadId/assistantId
- **AND** Response API → Chat Completions uses conversationId
- **AND** conversation history retrieved from ChatMemory (not database)

#### Scenario: Phase 2+ RAG document storage prepared
- **WHEN** RAG feature added in Phase 2
- **THEN** Document entity enhanced with pgvector embeddings
- **AND** VectorStore integration stores/queries embeddings for retrieval-augmented prompts
- **AND** no impact on conversation history (still in ChatMemory)

---

### Requirement: Spring Data JPA Repositories
The system SHALL use Spring Data JPA for data access layer (metadata and Phase 2+ RAG only).

Repository interfaces MUST:
- Extend JpaRepository<Entity, ID>
- Include custom query methods using @Query for metadata lookup
- Support pagination and sorting
- Implement TypeSafe Criteria API queries when needed

ConversationMetadataRepository:
```java
@Repository
public interface ConversationMetadataRepository extends JpaRepository<ConversationMetadata, String> {
    Optional<ConversationMetadata> findByConversationId(String conversationId);
    Optional<ConversationMetadata> findByThreadId(String threadId);
    Optional<ConversationMetadata> findByAssistantId(String assistantId);
    List<ConversationMetadata> findByProtocol(String protocol);
    List<ConversationMetadata> findByCreatedAtAfter(LocalDateTime date);
}

DocumentRepository (Phase 2+):
```java
@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {
    List<Document> findBySourceId(String sourceId);
    // Phase 2+: Add VectorStore integration for embedding similarity search
}
```

#### Scenario: Repository lookup ConversationMetadata by conversationId
- **WHEN** ProtocolRouter receives Chat Completions request with conversationId
- **THEN** ConversationMetadataRepository.findByConversationId() returns mapping metadata
- **AND** threadId/assistantId retrieved if this conversation was previously translated to Response API
- **AND** response history reconstructed from ChatMemory (not database)

#### Scenario: Repository lookup ConversationMetadata by threadId
- **WHEN** ProtocolRouter receives Response API request with threadId
- **THEN** ConversationMetadataRepository.findByThreadId() returns Chat Completions conversationId
- **AND** existing conversation history from ChatMemory is available for Chat Completions adapter

#### Scenario: Repository transaction handling for metadata
- **WHEN** saving ConversationMetadata (new protocol translation)
- **THEN** transaction commits atomically
- **AND** conversation history remains in ChatMemory (no database transaction needed)

---

### Requirement: Adapter Framework Implementation in Spring
The system SHALL implement adapter pattern using Spring beans and interfaces.

Adapter base interface:
```java
public interface ProtocolAdapter {
    boolean validate(Map<String, Object> request);
    Conversation requestToInternal(Map<String, Object> request);
    Map<String, Object> responseToExternal(CompletionResponse response);
    String getProtocolName();
    List<String> getSupportedModels();
}
```

AdapterRegistry using Spring:
```java
@Service
public class AdapterRegistry {
    private final Map<String, ProtocolAdapter> adapters = new ConcurrentHashMap<>();
    
    @Autowired
    private ChatCompletionAdapter chatAdapter;
    
    @Autowired
    private ResponseAPIAdapter responseApiAdapter;
    
    @PostConstruct
    public void registerDefaultAdapters() {
        register("openai-chat", chatAdapter);
        register("openai-response-api", responseApiAdapter);
    }
    
    public void register(String protocolName, ProtocolAdapter adapter) { ... }
    public ProtocolAdapter get(String protocolName) { ... }
    public List<String> listProtocols() { ... }
}
```

#### Scenario: Adapters auto-wired by Spring
- **WHEN** application context initializes
- **THEN** AdapterRegistry discovers and registers adapters automatically
- **AND** no manual configuration needed (except application.yml)

#### Scenario: Adapter registry accessed via dependency injection
- **WHEN** ProtocolRouter receives request
- **THEN** router injects AdapterRegistry and selects adapter by name

---

### Requirement: REST Controller for Router
The system SHALL expose router functionality via REST endpoints.

ProtocolRouterController:
```java
@RestController
@RequestMapping("/api/v1/router")
public class ProtocolRouterController {
    
    @Autowired
    private ProtocolRouter router;
    
    @PostMapping("/route")
    public ResponseEntity<?> route(
        @RequestBody Map<String, Object> request,
        @RequestHeader(value = "X-LLM-Protocol", required = false) String protocolHint
    ) {
        try {
            Map<String, Object> response = router.route(request, protocolHint);
            return ResponseEntity.ok(response);
        } catch (AdapterValidationException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(errorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of("status", "OK", "timestamp", LocalDateTime.now()));
    }
    
    @GetMapping("/protocols")
    public ResponseEntity<?> listProtocols() {
        return ResponseEntity.ok(adapterRegistry.listProtocols());
    }
}
```

#### Scenario: Route request via POST /api/v1/router/route
- **WHEN** POST /api/v1/router/route with Chat Completions format
- **THEN** response returned with 200 OK and result

#### Scenario: Protocol detection with header hint
- **WHEN** X-LLM-Protocol header provided
- **THEN** router uses specified protocol regardless of request format

---

### Requirement: Metadata State Store with PostgreSQL (NOT Conversation History)
The system SHALL implement ConversationMetadataStore interface with PostgreSQL backend.

**CRITICAL CLARIFICATION:** This stores ONLY protocol bridge metadata (conversationId ↔ threadId/assistantId mapping), NOT conversation history. Conversation history is managed by Spring AI ChatMemory and is NOT persisted to database.

ConversationMetadataStore interface:
```java
public interface ConversationMetadataStore {
    void saveMetadata(ConversationMetadata metadata);
    Optional<ConversationMetadata> getByConversationId(String conversationId);
    Optional<ConversationMetadata> getByThreadId(String threadId);
    void deleteMetadata(String conversationId);
    void cleanupOlderThan(Duration duration);
}
```

PostgreSQLMetadataStore implementation:
```java
@Service
public class PostgreSQLMetadataStore implements ConversationMetadataStore {
    
    @Autowired
    private ConversationMetadataRepository metadataRepository;
    
    @Override
    @Transactional
    public void saveMetadata(ConversationMetadata metadata) {
        metadata.setUpdatedAt(LocalDateTime.now());
        metadataRepository.save(metadata);
    }
    
    @Override
    public Optional<ConversationMetadata> getByConversationId(String conversationId) {
        return metadataRepository.findByConversationId(conversationId);
    }
    
    @Override
    public Optional<ConversationMetadata> getByThreadId(String threadId) {
        return metadataRepository.findByThreadId(threadId);
    }
    
    @Override
    @Transactional
    public void deleteMetadata(String conversationId) {
        metadataRepository.findByConversationId(conversationId)
            .ifPresent(m -> metadataRepository.delete(m));
    }
    
    @Override
    @Transactional
    @Scheduled(fixedDelay = 3600000) // Run every hour
    public void cleanupOlderThan(Duration duration) {
        LocalDateTime cutoff = LocalDateTime.now().minus(duration);
        List<ConversationMetadata> oldMetadata = metadataRepository.findByCreatedAtAfter(cutoff);
        metadataRepository.deleteAll(oldMetadata);
    }
}
```

#### Scenario: Save metadata when translating Chat→Response API
- **WHEN** ProtocolRouter receives Chat Completions request with conversationId
- **THEN** calls OpenAI Response API, gets threadId and assistantId
- **AND** ConversationMetadata saved with mapping: conversationId → threadId/assistantId
- **AND** ChatMemory already stores the conversation history (automatic)
- **AND** PostgreSQL stores ONLY the mapping metadata

#### Scenario: Retrieve metadata for protocol translation
- **WHEN** subsequent request arrives with conversationId or threadId
- **THEN** ConversationMetadataStore looks up the mapping
- **AND** returns threadId/assistantId (or conversationId) for protocol adapter
- **AND** conversation history retrieved from ChatMemory (not database)

#### Scenario: Auto-cleanup of old metadata
- **WHEN** @Scheduled cleanup task runs
- **THEN** ConversationMetadata records older than TTL are deleted
- **AND** associated ChatMemory entries should also be cleared (application-level cleanup)

---

### Requirement: Spring AI Integration for Chat
The system SHALL use Spring AI ChatClient for OpenAI integration with ChatMemory support.

Spring AI ChatClient with ChatMemory usage:
```java
@Service
public class ProtocolRouter {
    
    @Autowired
    private ChatClient chatClient; // Spring AI provided bean
    
    @Autowired
    private ChatMemory chatMemory; // Automatic conversation history storage
    
    @Autowired
    private ConversationMetadataStore metadataStore;
    
    public ChatResponse routeRequest(String conversationId, 
                                     String userMessage, 
                                     String protocol) {
        // Step 1: Load protocol mapping metadata
        ConversationMetadata metadata = loadMetadata(conversationId, protocol);
        
        // Step 2: Create message for current request
        Message userMsg = new Message(Role.USER, userMessage);
        
        // Step 3: Call ChatClient with ChatMemory
        // ChatMemory automatically:
        // - Retrieves previous messages by conversationId
        // - Includes them in the request to OpenAI
        // - Stores this new interaction after response
        ChatResponse response = chatClient.call(
            new Prompt(List.of(userMsg)),
            new ChatClient.ChatCallbacks() {
                @Override
                public void onBeforeCall(Prompt prompt) {
                    // ChatMemory has already injected history via conversationId
                }
            }
        );
        
        // Step 4: ChatMemory automatically stores this interaction
        // No explicit history save needed - ChatMemory handles it!
        
        // Step 5: Return response to adapter for protocol conversion
        return response;
    }
}
```

**KEY INSIGHT:** ChatMemory manages all conversation history automatically:
- Input: Automatically retrieves and includes previous messages for the conversation_id
- Output: Automatically stores the new interaction in ChatMemory
- No explicit database save needed for conversation history
- PostgreSQL only stores metadata (via ConversationMetadataStore)

Configuration in application.yml:
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
          temperature: 1.0
```

#### Scenario: ChatClient call with ChatMemory auto-history
- **WHEN** ProtocolRouter.routeRequest() called with conversationId and userMessage
- **THEN** ChatMemory automatically retrieves previous messages by conversationId
- **AND** ChatClient sends request with full message history to OpenAI
- **AND** OpenAI returns response
- **AND** ChatMemory automatically stores new interaction (user message + assistant response)

#### Scenario: No database round-trip for conversation history
- **WHEN** multiple requests arrive for same conversationId
- **THEN** ChatMemory provides message history instantly (in-memory)
- **AND** no PostgreSQL query needed for history (only for metadata)
- **AND** conversation history never persisted to database (PoC design)

---

### Requirement: Testing with JUnit 5 and Testcontainers
The system SHALL use JUnit 5 for unit tests and Testcontainers for integration tests.

Unit test example:
```java
@ExtendWith(MockitoExtension.class)
class ChatCompletionAdapterTest {
    
    private ChatCompletionAdapter adapter;
    
    @Mock
    private OpenAIChatService chatService;
    
    @BeforeEach
    void setUp() {
        adapter = new ChatCompletionAdapter(chatService);
    }
    
    @Test
    void testValidateChatRequest() {
        Map<String, Object> validRequest = Map.of(
            "messages", List.of(Map.of("role", "user", "content", "Hello")),
            "model", "gpt-4"
        );
        
        assertTrue(adapter.validate(validRequest));
    }
}
```

Integration test with Testcontainers:
```java
@SpringBootTest
@Testcontainers
class ConversationRepositoryTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
        .withDatabaseName("router_test")
        .withUsername("test")
        .withPassword("test");
    
    @Autowired
    private ConversationRepository repository;
    
    @Test
    void testSaveConversation() {
        Conversation conv = new Conversation();
        conv.setModel("gpt-4");
        conv.setCreatedAt(LocalDateTime.now());
        
        Conversation saved = repository.save(conv);
        
        assertNotNull(saved.getId());
        assertEquals("gpt-4", saved.getModel());
    }
}
```

#### Scenario: Unit tests run with Mockito
- **WHEN** running unit tests
- **THEN** adapters tested in isolation without database
- **AND** tests complete in seconds

#### Scenario: Integration tests run with real PostgreSQL
- **WHEN** running integration tests
- **THEN** Testcontainers spins up PostgreSQL
- **AND** full stack tested (Entities, Repositories, Services)

---

### Requirement: Configuration Management
The system SHALL use Spring Boot application.yml for configuration.

application.yml structure:
```yaml
spring:
  application:
    name: enterprise-llm-router
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:router}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
    hikari:
      maximum-pool-size: 10
  jpa:
    hibernate:
      ddl-auto: validate # Must create schema beforehand
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL10Dialect
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4

llm-router:
  adapter:
    chat-model-whitelist:
      - gpt-4
      - gpt-3.5-turbo
  conversation:
    cleanup-ttl-hours: 24
    cleanup-schedule: "0 0 * * *" # Daily at midnight
```

#### Scenario: Load configuration from environment
- **WHEN** application starts
- **THEN** reads OPENAI_API_KEY, DB_HOST, etc. from environment
- **AND** defaults applied if not provided

---

### Requirement: Error Handling with Exception Classes
The system SHALL define Spring-specific exception hierarchy.

Exception classes:
```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AdapterValidationException extends RuntimeException {
    public AdapterValidationException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class AdapterTransformationException extends RuntimeException {
    public AdapterTransformationException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ProtocolNotSupportedException extends RuntimeException {
    public ProtocolNotSupportedException(String protocol) {
        super("Protocol not supported: " + protocol);
    }
}
```

GlobalExceptionHandler:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AdapterValidationException.class)
    public ResponseEntity<?> handleValidation(AdapterValidationException e) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Validation Error",
            "message", e.getMessage(),
            "timestamp", LocalDateTime.now()
        ));
    }
}
```

#### Scenario: Validation error returns 400
- **WHEN** adapter validation fails
- **THEN** GlobalExceptionHandler catches exception
- **AND** returns 400 with error details

---

### Requirement: Logging with SLF4J
The system SHALL use SLF4J with Logback for logging.

Logging configuration (application.yml):
```yaml
logging:
  level:
    com.example.router: DEBUG
    org.springframework.ai: INFO
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

Usage in code:
```java
@Service
public class ProtocolRouter {
    private static final Logger logger = LoggerFactory.getLogger(ProtocolRouter.class);
    
    public Map<String, Object> route(Map<String, Object> request) {
        String requestId = UUID.randomUUID().toString();
        logger.info("Processing request: {}", requestId);
        
        try {
            // ... routing logic
            logger.debug("Request {} routed to adapter: {}", requestId, adapter.getProtocolName());
            return result;
        } catch (Exception e) {
            logger.error("Request {} failed: {}", requestId, e.getMessage());
            throw e;
        }
    }
}
```

#### Scenario: DEBUG logs show adapter selection
- **WHEN** request processed
- **THEN** DEBUG log shows protocol detection and adapter chosen

#### Scenario: ERROR logs include request ID
- **WHEN** exception occurs
- **THEN** log includes request ID for tracing

---

### Requirement: Build and Deployment
The system SHALL use Maven for building and packaging Spring Boot application.

pom.xml structure:
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>enterprise-llm-router</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <dependencies>
        <!-- Spring Boot starters -->
        <!-- Spring AI -->
        <!-- PostgreSQL -->
        <!-- Testing -->
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

Build commands:
```bash
mvn clean package
java -jar target/enterprise-llm-router-1.0.0.jar
```

#### Scenario: Maven builds executable JAR
- **WHEN** mvn clean package runs
- **THEN** creates enterprise-llm-router-1.0.0.jar
- **AND** jar executable with java -jar command

#### Scenario: Spring Boot runs embedded Tomcat
- **WHEN** jar executed
- **THEN** embedded Tomcat starts on port 8080
- **AND** Spring context loads with all beans
