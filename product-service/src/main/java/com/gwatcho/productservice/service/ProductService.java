package com.gwatcho.productservice.service;

import com.gwatcho.productservice.dto.*;
import com.gwatcho.productservice.entity.Product;
import com.gwatcho.productservice.entity.ProductStatus;
import com.gwatcho.productservice.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final OutboxService outboxService;


    @Transactional
    public ProductResponse createProduct(
            CreateProductRequest request) {

        if (productRepository.existsBySku(
                request.sku())) {

            throw new IllegalArgumentException(
                    "Product SKU already exists: "
                            + request.sku()
            );
        }

        Product product =
                Product.builder()
                        .sku(request.sku())
                        .name(request.name())
                        .description(request.description())
                        .price(request.price())
                        .currency(
                                request.currency()
                                        .toUpperCase()
                        )
                        .stockQuantity(
                                request.stockQuantity()
                        )
                        .category(request.category())
                        .status(
                                request.stockQuantity() > 0
                                        ? ProductStatus.ACTIVE
                                        : ProductStatus.OUT_OF_STOCK
                        )
                        .build();

        Product savedProduct =
                productRepository.save(product);

        outboxService.createProductCreatedEvent(
                savedProduct
        );

        return toResponse(savedProduct);
    }


    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {

        return toResponse(
                findProduct(id)
        );
    }


    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts() {

        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveProducts() {

        return productRepository
                .findByStatus(ProductStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(
            String category) {

        return productRepository
                .findByCategoryIgnoreCase(category)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional
    public ProductResponse updateProduct(
            Long id,
            UpdateProductRequest request) {

        Product product =
                findProduct(id);

        product.setName(request.name());

        product.setDescription(
                request.description()
        );

        product.setPrice(
                request.price()
        );

        product.setCurrency(
                request.currency().toUpperCase()
        );

        product.setCategory(
                request.category()
        );

        return toResponse(
                productRepository.save(product)
        );
    }


    @Transactional
    public void deactivateProduct(Long id) {

        Product product =
                findProduct(id);

        product.setStatus(
                ProductStatus.INACTIVE
        );

        productRepository.save(product);
    }


    @Transactional
    public ProductResponse addStock(
            Long id,
            int quantity) {

        Product product =
                findProduct(id);

        product.increaseStock(quantity);

        return toResponse(
                productRepository.save(product)
        );
    }


    @Transactional
    public ProductResponse removeStock(
            Long id,
            int quantity) {

        Product product = findProduct(id);
        product.decreaseStock(quantity);
        return toResponse(
                productRepository.save(product)
        );
    }


    private Product findProduct(Long id) {

        return productRepository.findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Product not found: " + id
                        )
                );
    }


    private ProductResponse toResponse(
            Product product) {

        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency(),
                product.getStockQuantity(),
                product.getStatus(),
                product.getCategory(),
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}