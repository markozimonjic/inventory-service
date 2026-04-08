# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start infrastructure (PostgreSQL + Kafka)
docker compose up -d

# Run the application
./mvnw spring-boot:run

# Build
./mvnw package

# Run all tests (requires Docker for Testcontainers)
./mvnw test

# Run a single test class
./mvnw test -Dtest=ProductServiceTest

# Run a single test method
./mvnw test -Dtest=ProductServiceTest#increaseStock_updates_quantity_and_publishes_event
```

## Architecture

This is a Spring Boot 3.3 / Java 21 microservice. The domain is a product catalogue with stock management. The request flow is:

**REST → Controller → Service → Repository + EventPublisher**

- `ProductController` maps HTTP requests to `ProductService` calls and returns `ProductResponse` DTOs.
- `ProductService` owns all business rules (duplicate SKU check, insufficient-stock guard, optimistic-lock semantics). After every stock mutation it publishes a `StockChangedEvent` via Spring's `ApplicationEventPublisher`. `StockEventPublisher` listens with `@TransactionalEventListener(phase = AFTER_COMMIT)` and forwards to Kafka — guaranteeing the event is sent only if the DB transaction committed successfully.
- `Product` is a JPA entity with `@Version` for optimistic locking. Stock mutations are methods on the entity (`increaseStock` / `decreaseStock`) rather than setters.
- `OrderEventConsumer` listens on `orders.created`, iterates each order line, and calls `ProductService.decreaseStock`. Failures per line are logged as warnings and do not stop processing of remaining lines.

**Kafka wiring** (`KafkaConfig`):
- Two `NewTopic` beans auto-create `inventory.stock-changed` (produced) and `orders.created` (consumed), both with 3 partitions.
- Producer uses `JsonSerializer`; consumer uses a typed `JsonDeserializer<OrderCreatedEvent>` with its own `ConsumerFactory` and `ConcurrentKafkaListenerContainerFactory`.
- Topic names are externalized in `application.yml` under `inventory.topics.*`.

**Error handling**: `GlobalExceptionHandler` (`@RestControllerAdvice`) maps domain exceptions to HTTP status codes and returns a uniform `ApiError` body. `ProductNotFoundException` → 404, `InsufficientStockException` → 409, `DuplicateSkuException` → 409, Bean Validation failures → 400.

**Database migrations**: Flyway manages schema under `src/main/resources/db/migration/`. JPA is set to `ddl-auto: validate` — Hibernate never modifies the schema, Flyway owns it.

## Testing approach

- **Unit tests** (`ProductServiceTest`): Mockito, no Spring context. Test `ProductService` in isolation by mocking `ProductRepository` and `ApplicationEventPublisher`.
- **Integration tests** (`ProductControllerIT`): Full `@SpringBootTest` with `MockMvc`. Uses Testcontainers (`PostgreSQLContainer`) for a real database and `@EmbeddedKafka` for an in-process broker. Docker must be running.
- **Integration tests** (`OrderEventConsumerIT`): Sends a real Kafka message via `KafkaTemplate` and uses Awaitility to assert the consumer processed it. Also covers the unknown-SKU case.

## Configuration

Override defaults via environment variables:

| Variable                  | Default                                      |
|---------------------------|----------------------------------------------|
| `DB_URL`                  | `jdbc:postgresql://localhost:5432/inventory` |
| `DB_USER`                 | `inventory`                                  |
| `DB_PASSWORD`             | `inventory`                                  |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092`                             |
| `SERVER_PORT`             | `8080`                                       |

Swagger UI: `http://localhost:8080/swagger-ui.html`  
Actuator health: `http://localhost:8080/actuator/health`
