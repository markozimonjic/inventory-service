package com.markozimonjic.inventory.product.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message) {
        super(message);
    }

    public static ProductNotFoundException bySku(String sku) {
        return new ProductNotFoundException("Product with SKU '" + sku + "' not found");
    }

}
