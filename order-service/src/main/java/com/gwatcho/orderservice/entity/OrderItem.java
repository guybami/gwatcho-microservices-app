package com.gwatcho.orderservice.entity;

import jakarta.persistence.*;

import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "order_id",
            nullable = false
    )
    private Order order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 255)
    private String productName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    public void calculateLineTotal() {

        lineTotal =
                unitPrice.multiply(
                        BigDecimal.valueOf(quantity)
                );
    }
}