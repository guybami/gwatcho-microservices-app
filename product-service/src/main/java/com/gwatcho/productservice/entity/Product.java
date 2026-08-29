package com.gwatcho.productservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_sku",
                        columnNames = "sku"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            length = 50
    )
    private String sku;

    @Column(
            nullable = false,
            length = 255
    )
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal price;

    @Column(
            nullable = false,
            length = 3
    )
    private String currency;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(
            nullable = false,
            length = 100
    )
    private String category;

    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;


    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = ProductStatus.ACTIVE;
        }
        updateStockStatus();
    }


    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateStockStatus();
    }


    public void updateStockStatus() {
        if (stockQuantity != null
                && stockQuantity == 0
                && status == ProductStatus.ACTIVE) {

            status = ProductStatus.OUT_OF_STOCK;
        }
    }


    public boolean isAvailable(int requestedQuantity) {
        return status == ProductStatus.ACTIVE
                && stockQuantity != null
                && stockQuantity >= requestedQuantity;
    }


    public void decreaseStock(int quantity) {
        if (!isAvailable(quantity)) {

            throw new IllegalStateException(
                    "Insufficient stock for product "
                            + id
            );
        }
        stockQuantity -= quantity;
        updateStockStatus();
    }


    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }
        stockQuantity += quantity;
        if (status == ProductStatus.OUT_OF_STOCK) {
            status = ProductStatus.ACTIVE;
        }
    }
}