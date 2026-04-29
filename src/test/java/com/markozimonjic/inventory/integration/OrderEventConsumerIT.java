package com.markozimonjic.inventory.integration;

import com.markozimonjic.inventory.messaging.OrderCreatedEvent;
import com.markozimonjic.inventory.product.Product;
import com.markozimonjic.inventory.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"inventory.stock-changed", "orders.created"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
class OrderEventConsumerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanUp() {
        productRepository.deleteAll();
    }

    @Test
    void order_event_decreases_stock() {
        productRepository.save(new Product("CONS-1", "Consumer test product", 20));

        var event = new OrderCreatedEvent(
                "ORD-001",
                List.of(new OrderCreatedEvent.OrderLine("CONS-1", 5)),
                Instant.now()
        );
        kafkaTemplate.send("orders.created", "ORD-001", event);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Product product = productRepository.findBySku("CONS-1").orElseThrow();
            assertThat(product.getQuantity()).isEqualTo(15);
        });
    }

    @Test
    void order_event_with_unknown_sku_is_skipped_gracefully() {
        var event = new OrderCreatedEvent(
                "ORD-002",
                List.of(new OrderCreatedEvent.OrderLine("NO-SUCH-SKU", 1)),
                Instant.now()
        );

        // should not throw — consumer logs a warning and continues
        kafkaTemplate.send("orders.created", "ORD-002", event);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(productRepository.findBySku("NO-SUCH-SKU")).isEmpty()
        );
    }
}
