package com.markozimonjic.inventory.product;

import com.markozimonjic.inventory.product.dto.CreateProductRequest;
import com.markozimonjic.inventory.product.dto.ProductResponse;
import com.markozimonjic.inventory.product.dto.UpdateStockRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> list() {
        return productService.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/{sku}")
    public ProductResponse get(@PathVariable String sku) {
        return ProductResponse.from(productService.getBySku(sku));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        var product = productService.create(request.sku(), request.name(), request.quantity());
        URI location = uriBuilder.path("/api/products/{sku}").buildAndExpand(product.getSku()).toUri();
        return ResponseEntity.created(location).body(ProductResponse.from(product));
    }

    @PostMapping("/{sku}/stock/increase")
    public ProductResponse increase(@PathVariable String sku,
                                    @Valid @RequestBody UpdateStockRequest request) {
        return ProductResponse.from(productService.increaseStock(sku, request.amount()));
    }

    @PostMapping("/{sku}/stock/decrease")
    public ProductResponse decrease(@PathVariable String sku,
                                    @Valid @RequestBody UpdateStockRequest request) {
        return ProductResponse.from(productService.decreaseStock(sku, request.amount()));
    }
}
