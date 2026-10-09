package com.gwatcho.productservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gwatcho.productservice.entity.Product;
import com.gwatcho.productservice.entity.ProductStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProductRepositoryTest {
    @Autowired private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindProductBySku() {
        Product product = createProduct("SKU-001", "Test Product", "Test description", new BigDecimal("99.99"), "EUR", 10,
                ProductStatus.ACTIVE, "ELECTRONICS");

        Product savedProduct = productRepository.save(product);

        Optional<Product> result = productRepository.findBySku("SKU-001");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedProduct.getId());
        assertThat(result.get().getSku()).isEqualTo("SKU-001");
        assertThat(result.get().getName()).isEqualTo("Test Product");
        assertThat(result.get().getPrice()).isEqualByComparingTo("99.99");
        assertThat(result.get().getCurrency()).isEqualTo("EUR");
        assertThat(result.get().getStockQuantity()).isEqualTo(10);
        assertThat(result.get().getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(result.get().getCategory()).isEqualTo("ELECTRONICS");
    }

    @Test
    void shouldReturnEmptyWhenSkuDoesNotExist() {
        Optional<Product> result = productRepository.findBySku("UNKNOWN-SKU");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldCheckIfSkuExists() {
        Product product = createProduct("SKU-002", "Existing Product", "Test description", new BigDecimal("49.99"), "EUR",
                5, ProductStatus.ACTIVE, "BOOKS");

        productRepository.save(product);

        assertThat(productRepository.existsBySku("SKU-002")).isTrue();
        assertThat(productRepository.existsBySku("UNKNOWN-SKU")).isFalse();
    }

    @Test
    void shouldFindProductsByStatus() {
        Product activeProduct = createProduct("SKU-003", "Active Product", "Active product", new BigDecimal("100.00"),
                "EUR", 10, ProductStatus.ACTIVE, "ELECTRONICS");

        Product inactiveProduct = createProduct("SKU-004", "Inactive Product", "Inactive product", new BigDecimal("200.00"),
                "EUR", 10, ProductStatus.INACTIVE, "ELECTRONICS");

        productRepository.save(activeProduct);
        productRepository.save(inactiveProduct);

        List<Product> activeProducts = productRepository.findByStatus(ProductStatus.ACTIVE);

        assertThat(activeProducts)
                .hasSize(1)
                .allMatch(product -> product.getSku().equals("SKU-003") && product.getStatus() == ProductStatus.ACTIVE);
    }

    @Test
    void shouldFindProductsByCategoryIgnoreCase() {
        Product product1 = createProduct("SKU-005", "Laptop", "Business laptop", new BigDecimal("1200.00"), "EUR", 10,
                ProductStatus.ACTIVE, "Computers");

        Product product2 = createProduct("SKU-006", "Monitor", "Computer monitor", new BigDecimal("350.00"), "EUR", 5,
                ProductStatus.ACTIVE, "COMPUTERS");

        Product product3 = createProduct(
                "SKU-007", "Chair", "Office chair", new BigDecimal("250.00"), "EUR", 8, ProductStatus.ACTIVE, "Furniture");

        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);

        List<Product> products = productRepository.findByCategoryIgnoreCase("computers");

        assertThat(products).hasSize(2).extracting(Product::getSku).containsExactlyInAnyOrder("SKU-005", "SKU-006");
    }

    @Test
    void shouldCreateTimestampsAutomatically() {
        Product product = createProduct("SKU-008", "Timestamp Product", "Testing timestamps", new BigDecimal("25.00"),
                "EUR", 10, ProductStatus.ACTIVE, "TEST");

        Product savedProduct = productRepository.saveAndFlush(product);

        assertThat(savedProduct.getCreatedAt()).isNotNull();
        assertThat(savedProduct.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldSetOutOfStockWhenStockIsZero() {
        Product product = createProduct("SKU-009", "Out Of Stock Product", "Testing stock status", new BigDecimal("30.00"),
                "EUR", 0, ProductStatus.ACTIVE, "TEST");

        Product savedProduct = productRepository.saveAndFlush(product);

        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    private Product createProduct(String sku, String name, String description, BigDecimal price, String currency,
                                  Integer stockQuantity, ProductStatus status, String category) {
        Product product = new Product();

        product.setSku(sku);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCurrency(currency);
        product.setStockQuantity(stockQuantity);
        product.setStatus(status);
        product.setCategory(category);

        return product;
    }
}