# inventory-service

![CI](https://github.com/markozimonjic/inventory-service/actions/workflows/ci.yml/badge.svg)

> **Portfolio project** — built to demonstrate event-driven microservice design with Spring Boot and Kafka. Not intended for production use.

Event-driven inventory microservice built with **Spring Boot 3** and **Apache Kafka**.

It manages a product catalogue with stock levels, exposes a REST API to query and adjust stock, publishes a `StockChangedEvent` whenever stock moves, and consumes `OrderCreatedEvent` messages from an upstream order service to automatically reserve stock.

The project intentionally covers several patterns that come up in real backend systems: optimistic locking, transactional event publishing, centralized error handling, database migrations, and integration testing with real infrastructure via Testcontainers.

## Features

- REST API for products and stock operations (`/api/products`)
- PostgreSQL persistence with Flyway migrations
- Optimistic locking on the `Product` entity (`@Version`)
- Kafka producer for `inventory.stock-changed`
- Kafka consumer for `orders.created`
- Bean Validation on incoming requests
- Centralized error handling via `@RestControllerAdvice`
- OpenAPI / Swagger UI at `/swagger-ui.html`
- Spring Boot Actuator health endpoints (`/actuator/health`)
- Unit tests with Mockito, integration tests with Testcontainers + embedded Kafka

## Tech stack

| Layer        | Choice                                  |
|--------------|-----------------------------------------|
| Language     | Java 21                                 |
| Framework    | Spring Boot 3.3                         |
| Persistence  | Spring Data JPA, Hibernate, PostgreSQL  |
| Migrations   | Flyway                                  |
| Messaging    | Spring Kafka                            |
| API docs     | springdoc-openapi                       |
| Testing      | JUnit 5, Mockito, Testcontainers        |
| Build        | Maven                                   |

## Running locally

Requirements: JDK 21, Docker, Maven 3.9+.

```bash
# Start Postgres + Kafka
docker compose up -d

# Run the service
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`.

Swagger UI: `http://localhost:8080/swagger-ui.html`

## API examples

Create a product:

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"sku":"BOOK-001","name":"Effective Java","quantity":50}'
```

Increase stock:

```bash
curl -X POST http://localhost:8080/api/products/BOOK-001/stock/increase \
  -H "Content-Type: application/json" \
  -d '{"amount":10}'
```

Decrease stock:

```bash
curl -X POST http://localhost:8080/api/products/BOOK-001/stock/decrease \
  -H "Content-Type: application/json" \
  -d '{"amount":3}'
```

List products:

```bash
curl http://localhost:8080/api/products
```

## Kafka topics

| Topic                     | Direction | Payload               |
|---------------------------|-----------|-----------------------|
| `inventory.stock-changed` | produced  | `StockChangedEvent`   |
| `orders.created`          | consumed  | `OrderCreatedEvent`   |

Example payload for `orders.created`:

```json
{
  "orderId": "ORD-123",
  "lines": [
    { "sku": "BOOK-001", "quantity": 2 }
  ],
  "occurredAt": "2026-01-15T10:21:00Z"
}
```

## Tests

```bash
./mvnw test
```

Integration tests require Docker to be running (Testcontainers starts a real PostgreSQL container).

The suite includes:
- **Unit tests** (`ProductServiceTest`) — Mockito, no Spring context
- **Integration tests** (`ProductControllerIT`) — full stack with Testcontainers (real PostgreSQL) and embedded Kafka
- **Integration tests** (`OrderEventConsumerIT`) — sends a real Kafka message and asserts stock is decremented

## Configuration

All connection details can be overridden via environment variables:

| Variable                  | Default                                       |
|---------------------------|-----------------------------------------------|
| `DB_URL`                  | `jdbc:postgresql://localhost:5432/inventory`  |
| `DB_USER`                 | `inventory`                                   |
| `DB_PASSWORD`             | `inventory`                                   |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092`                              |
| `SERVER_PORT`             | `8080`                                        |

## Project structure

```
src/main/java/com/markozimonjic/inventory/
├── InventoryServiceApplication.java
├── config/             # Kafka topic + (de)serializer configuration
├── messaging/          # Producers, consumers and event records
├── product/            # Domain entity, repository, service, controller, DTOs
└── web/                # Global exception handler and API error model
```
