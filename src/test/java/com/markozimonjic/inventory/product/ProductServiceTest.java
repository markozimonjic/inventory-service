package com.markozimonjic.inventory.product;

import com.markozimonjic.inventory.messaging.StockChangedEvent;
import com.markozimonjic.inventory.messaging.StockEventPublisher;
import com.markozimonjic.inventory.messaging.StockOperation;
import com.markozimonjic.inventory.product.exception.InsufficientStockException;
import com.markozimonjic.inventory.product.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private StockEventPublisher eventPublisher;

    @InjectMocks
    private ProductService service;

    private Product existing;

    @BeforeEach
    void setUp() {
        existing = new Product("SKU-1", "Test product", 10);
    }

    @Test
    void create_persists_new_product() {
        when(repository.existsBySku("SKU-2")).thenReturn(false);
        when(repository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = service.create("SKU-2", "New", 5);

        assertThat(result.getSku()).isEqualTo("SKU-2");
        assertThat(result.getQuantity()).isEqualTo(5);
        verify(repository).save(any(Product.class));
    }

    @Test
    void create_rejects_duplicate_sku() {
        when(repository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create("SKU-1", "x", 1))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void getBySku_throws_when_missing() {
        when(repository.findBySku("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySku("missing"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void increaseStock_updates_quantity_and_publishes_event() {
        when(repository.findBySku("SKU-1")).thenReturn(Optional.of(existing));

        Product result = service.increaseStock("SKU-1", 3);

        assertThat(result.getQuantity()).isEqualTo(13);

        ArgumentCaptor<StockChangedEvent> captor = ArgumentCaptor.forClass(StockChangedEvent.class);
        verify(eventPublisher, times(1)).publish(captor.capture());
        StockChangedEvent event = captor.getValue();
        assertThat(event.sku()).isEqualTo("SKU-1");
        assertThat(event.previousQuantity()).isEqualTo(10);
        assertThat(event.newQuantity()).isEqualTo(13);
        assertThat(event.operation()).isEqualTo(StockOperation.INCREASE);
    }

    @Test
    void decreaseStock_throws_when_amount_exceeds_quantity() {
        when(repository.findBySku("SKU-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.decreaseStock("SKU-1", 50))
                .isInstanceOf(InsufficientStockException.class);

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void decreaseStock_publishes_event_on_success() {
        when(repository.findBySku("SKU-1")).thenReturn(Optional.of(existing));

        Product result = service.decreaseStock("SKU-1", 4);

        assertThat(result.getQuantity()).isEqualTo(6);
        verify(eventPublisher).publish(any(StockChangedEvent.class));
    }
}
