package com.markozimonjic.inventory.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${inventory.topics.stock-changed}")
    private String topic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(StockChangedEvent event) {
        kafkaTemplate.send(topic, event.sku(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish StockChangedEvent for sku={}", event.sku(), ex);
                    } else {
                        log.info("Published StockChangedEvent sku={} offset={}",
                                event.sku(), result.getRecordMetadata().offset());
                    }
                });
    }
}
