package com.markozimonjic.inventory.messaging;

import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(
        String orderId,
        List<OrderLine> lines,
        Instant occurredAt
) {
    public record OrderLine(String sku, int quantity) {
    }
}
