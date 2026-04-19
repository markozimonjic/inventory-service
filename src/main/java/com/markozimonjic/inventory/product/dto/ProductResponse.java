package com.markozimonjic.inventory.product.dto;

import com.markozimonjic.inventory.product.Product;

import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        int quantity,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getQuantity(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
