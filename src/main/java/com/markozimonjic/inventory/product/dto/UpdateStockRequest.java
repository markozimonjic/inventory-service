package com.markozimonjic.inventory.product.dto;

import jakarta.validation.constraints.Min;

public record UpdateStockRequest(
        @Min(1) int amount
) {
}
