package com.markozimonjic.inventory.product.exception;

public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String sku) {
        super("Product with SKU '" + sku + "' already exists");
    }
}
