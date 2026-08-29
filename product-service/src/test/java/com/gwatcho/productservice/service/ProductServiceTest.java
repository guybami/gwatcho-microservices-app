package com.gwatcho.productservice.service;

import com.gwatcho.productservice.dto.CreateProductRequest;
import com.gwatcho.productservice.dto.ProductResponse;
import com.gwatcho.productservice.dto.UpdateProductRequest;
import com.gwatcho.productservice.entity.Product;
import com.gwatcho.productservice.entity.ProductStatus;
import com.gwatcho.productservice.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private ProductService productService;

    private Product product;


    @BeforeEach
    void setUp() {

        product = Product.builder()
                .id(1L)
                .sku("LAPTOP-001")
                .name("Lenovo Laptop")
                .description("Business laptop")
                .price(new BigDecimal("1200.00"))
                .currency("EUR")
                .stockQuantity(25)
                .status(ProductStatus.ACTIVE)
                .category("COMPUTERS")
                .build();
    }

    @Test
    void shouldCreateProduct() {

        CreateProductRequest request =
                new CreateProductRequest(
                        "LAPTOP-001",
                        "Lenovo Laptop",
                        "Business laptop",
                        new BigDecimal("1200.00"),
                        "EUR",
                        25,
                        "COMPUTERS"
                );

        when(productRepository.existsBySku("LAPTOP-001"))
                .thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        ProductResponse response =
                productService.createProduct(request);

        assertThat(response).isNotNull();

        assertThat(response.sku())
                .isEqualTo("LAPTOP-001");

        verify(productRepository)
                .save(any(Product.class));

        verify(outboxService)
                .createProductCreatedEvent(
                        any(Product.class)
                );
    }

    @Test
    void shouldRejectDuplicateSku() {

        CreateProductRequest request =
                new CreateProductRequest(
                        "LAPTOP-001",
                        "Another Laptop",
                        null,
                        new BigDecimal("1000.00"),
                        "EUR",
                        10,
                        "COMPUTERS"
                );

        when(productRepository.existsBySku("LAPTOP-001"))
                .thenReturn(true);

        assertThatThrownBy(
                () -> productService.createProduct(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Product SKU already exists"
                );

        verify(productRepository, never())
                .save(any(Product.class));
    }


    @Test
    void shouldGetProductById() {

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        ProductResponse response =
                productService.getProduct(1L);

        assertThat(response.id())
                .isEqualTo(1L);

        assertThat(response.name())
                .isEqualTo("Lenovo Laptop");

        assertThat(response.stockQuantity())
                .isEqualTo(25);

        verify(productRepository)
                .findById(1L);
    }

    @Test
    void shouldThrowWhenProductDoesNotExist() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> productService.getProduct(999L)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(
                        "Product not found"
                );
    }

    @Test
    void shouldGetAllProducts() {

        Product secondProduct =
                Product.builder()
                        .id(2L)
                        .sku("KEYBOARD-001")
                        .name("Keyboard")
                        .price(new BigDecimal("80.00"))
                        .currency("EUR")
                        .stockQuantity(100)
                        .status(ProductStatus.ACTIVE)
                        .category("ACCESSORIES")
                        .build();

        when(productRepository.findAll())
                .thenReturn(
                        List.of(
                                product,
                                secondProduct
                        )
                );

        List<ProductResponse> result =
                productService.getProducts();

        assertThat(result)
                .hasSize(2);

        assertThat(result.get(0).sku())
                .isEqualTo("LAPTOP-001");

        assertThat(result.get(1).sku())
                .isEqualTo("KEYBOARD-001");

        verify(productRepository)
                .findAll();
    }

    @Test
    void shouldUpdateProduct() {

        UpdateProductRequest request =
                new UpdateProductRequest(
                        "Lenovo ThinkPad",
                        "Updated description",
                        new BigDecimal("1300.00"),
                        "EUR",
                        "COMPUTERS"
                );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        ProductResponse response =
                productService.updateProduct(
                        1L,
                        request
                );

        assertThat(product.getName())
                .isEqualTo("Lenovo ThinkPad");

        assertThat(product.getPrice())
                .isEqualByComparingTo("1300.00");

        assertThat(product.getCategory())
                .isEqualTo("COMPUTERS");

        verify(productRepository)
                .save(product);
    }

    @Test
    void shouldDeactivateProduct() {

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        productService.deactivateProduct(1L);

        assertThat(product.getStatus())
                .isEqualTo(ProductStatus.INACTIVE);

        verify(productRepository)
                .save(product);
    }

    @Test
    void shouldAddStock() {

        product.setStockQuantity(10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        ProductResponse response =
                productService.addStock(1L, 5);

        assertThat(response.stockQuantity())
                .isEqualTo(15);

        assertThat(response.status())
                .isEqualTo(ProductStatus.ACTIVE);

        verify(productRepository)
                .save(product);
    }

    @Test
    void shouldRemoveStock() {

        product.setStockQuantity(10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        ProductResponse response =
                productService.removeStock(1L, 4);

        assertThat(response.stockQuantity())
                .isEqualTo(6);

        verify(productRepository)
                .save(product);
    }

    @Test
    void shouldMarkProductOutOfStock() {

        product.setStockQuantity(5);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        ProductResponse response =
                productService.removeStock(1L, 5);

        assertThat(response.stockQuantity())
                .isZero();

        assertThat(response.status())
                .isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    void shouldRejectRemovingMoreStockThanAvailable() {

        product.setStockQuantity(5);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(
                () -> productService.removeStock(1L, 10)
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessageContaining(
                        "Insufficient stock"
                );

        verify(productRepository, never())
                .save(any(Product.class));
    }

    @Test
    void shouldReactivateProductWhenStockIsAdded() {

        product.setStockQuantity(0);
        product.setStatus(
                ProductStatus.OUT_OF_STOCK
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        ProductResponse response =
                productService.addStock(1L, 10);

        assertThat(response.stockQuantity())
                .isEqualTo(10);

        assertThat(response.status())
                .isEqualTo(ProductStatus.ACTIVE);
    }
}

