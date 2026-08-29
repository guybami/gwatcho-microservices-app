package com.gwatcho.productservice.controller;

import com.gwatcho.productservice.dto.*;
import com.gwatcho.productservice.service.ProductService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;


    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody
            CreateProductRequest request) {
        try {
            ProductResponse productResponse = productService.createProduct(request);
            if(productResponse != null) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(productResponse);
            } else {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }


    @GetMapping
    public ResponseEntity<List<ProductResponse>>
    getProducts() {

        return ResponseEntity.ok(
                productService.getProducts()
        );
    }


    @GetMapping("/active")
    public ResponseEntity<List<ProductResponse>>   getActiveProducts() {
        return ResponseEntity.ok(
                productService.getActiveProducts()
        );
    }


    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>>
    getProductsByCategory(
            @PathVariable String category) {

        return ResponseEntity.ok(
                productService.getProductsByCategory(
                        category
                )
        );
    }


    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse>   updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody
            UpdateProductRequest request) {

        return ResponseEntity.ok(
                productService.updateProduct(
                        id,
                        request
                )
        );
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateProduct(
            @PathVariable Long id) {

        productService.deactivateProduct(id);

        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/stock/add")
    public ResponseEntity<ProductResponse>
    addStock(
            @PathVariable Long id,
            @RequestParam int quantity) {

        return ResponseEntity.ok(
                productService.addStock(
                        id,
                        quantity
                )
        );
    }


    @PostMapping("/{id}/stock/remove")
    public ResponseEntity<ProductResponse>
    removeStock(
            @PathVariable Long id,
            @RequestParam int quantity) {

        return ResponseEntity.ok(
                productService.removeStock(
                        id,
                        quantity
                )
        );
    }
}