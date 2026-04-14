package com.markozimonjic.inventory.messaging;

import com.markozimonjic.inventory.product.Product;

import java.time.Instant;

public record StockChangedEvent(
        String sku,
        int previousQuantity,
        int newQuantity,
        StockOperation operation,
        Instant occurredAt
) {
    public static StockChangedEvent of(Product product, int previousQuantity, StockOperation operation) {
        return new StockChangedEvent(
                product.getSku(),
                previousQuantity,
                product.getQuantity(),
                operation,
                Instant.now()
        );
    }
}
