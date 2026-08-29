package com.gwatcho.productservice.repository;

import com.gwatcho.productservice.entity.Product;
import com.gwatcho.productservice.entity.ProductStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategoryIgnoreCase(
            String category
    );
}