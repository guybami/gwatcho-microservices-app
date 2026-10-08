package com.gwatcho.productservice.integration;

import com.gwatcho.productservice.entity.Product;
import com.gwatcho.productservice.entity.ProductStatus;

import com.gwatcho.productservice.repository.ProductRepository;
import jakarta.persistence.EntityManager;

import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;


@DataJpaTest
class ProductRepositoryIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private TransactionTemplate transactionTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;



    @BeforeEach
    void setUp() {
        transactionTemplate =
                new TransactionTemplate(transactionManager);
    }



    @Test
    void shouldPersistProductWithVersion() {

        Product product = Product.builder()
                .sku("TEST-001")
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .currency("EUR")
                .stockQuantity(10)
                .status(ProductStatus.ACTIVE)
                .category("TEST")
                .build();

        Product saved = productRepository.saveAndFlush(product);
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getVersion()).isZero();
    }


    //@Test
    void shouldDetectOptimisticLockConflict() {

        // Create product in its own transaction
        Long productId = transactionTemplate.execute(status -> {

            Product product = Product.builder()
                    .sku("TEST-002")
                    .name("Test Product")
                    .price(new BigDecimal("100.00"))
                    .currency("EUR")
                    .stockQuantity(10)
                    .status(ProductStatus.ACTIVE)
                    .category("TEST")
                    .build();

            Product saved =
                    productRepository.save(product);

            return saved.getId();
        });


        // Load product A
        Product productA =
                transactionTemplate.execute(status ->
                        productRepository.findById(productId)
                                .orElseThrow()
                );


        // Load product B
        Product productB =
                transactionTemplate.execute(status ->
                        productRepository.findById(productId)
                                .orElseThrow()
                );


        assertThat(productA.getVersion())
                .isEqualTo(productB.getVersion());


        /*
         * Transaction A
         *
         * version: 0 -> 1
         */
        transactionTemplate.execute(status -> {

            Product current =
                    productRepository.findById(productId)
                            .orElseThrow();

            current.setStockQuantity(9);

            productRepository.saveAndFlush(current);

            return null;
        });


        /*
         * productB contains the OLD version.
         */
        assertThat(productB.getVersion())
                .isEqualTo(1L);


        /*
         * Transaction B tries to update
         * using the stale version.
         */
        assertThatThrownBy(() ->

                transactionTemplate.execute(status -> {

                    productRepository.saveAndFlush(productB);

                    return null;
                })

        ).hasRootCauseInstanceOf(
                OptimisticLockException.class
        );
    }


}