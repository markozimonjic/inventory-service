package com.markozimonjic.inventory.messaging;

import com.markozimonjic.inventory.product.ProductService;
import com.markozimonjic.inventory.product.exception.InsufficientStockException;
import com.markozimonjic.inventory.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ProductService productService;

    @KafkaListener(
            topics = "${inventory.topics.order-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent orderId={} lines={}", event.orderId(), event.lines().size());
        for (OrderCreatedEvent.OrderLine line : event.lines()) {
            try {
                productService.decreaseStock(line.sku(), line.quantity());
            } catch (ProductNotFoundException | InsufficientStockException ex) {
                log.warn("Cannot reserve stock for orderId={} sku={}: {}",
                        event.orderId(), line.sku(), ex.getMessage());
            }
        }
    }
}
