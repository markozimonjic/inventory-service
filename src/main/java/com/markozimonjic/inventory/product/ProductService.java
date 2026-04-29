package com.markozimonjic.inventory.product;

import com.markozimonjic.inventory.messaging.StockChangedEvent;
import com.markozimonjic.inventory.messaging.StockEventPublisher;
import com.markozimonjic.inventory.messaging.StockOperation;
import com.markozimonjic.inventory.product.exception.InsufficientStockException;
import com.markozimonjic.inventory.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository repository;
    private final StockEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Product getBySku(String sku) {
        return repository.findBySku(sku)
                .orElseThrow(() -> ProductNotFoundException.bySku(sku));
    }

    @Transactional
    public Product create(String sku, String name, int quantity) {
        if (repository.existsBySku(sku)) {
            throw new IllegalArgumentException("Product with SKU '" + sku + "' already exists");
        }
        Product saved = repository.save(new Product(sku, name, quantity));
        log.info("Created product sku={} initialQuantity={}", saved.getSku(), saved.getQuantity());
        return saved;
    }

    @Transactional
    public Product increaseStock(String sku, int amount) {
        Product product = getBySku(sku);
        int previous = product.getQuantity();
        product.increaseStock(amount);
        eventPublisher.publish(StockChangedEvent.of(product, previous, StockOperation.INCREASE));
        return product;
    }

    @Transactional
    public Product decreaseStock(String sku, int amount) {
        Product product = getBySku(sku);
        if (amount > product.getQuantity()) {
            throw new InsufficientStockException(sku, amount, product.getQuantity());
        }
        int previous = product.getQuantity();
        product.decreaseStock(amount);
        eventPublisher.publish(StockChangedEvent.of(product, previous, StockOperation.DECREASE));
        return product;
    }
}
