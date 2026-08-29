package com.gwatcho.productservice.controller;



import com.fasterxml.jackson.databind.ObjectMapper;

import com.gwatcho.productservice.dto.CreateProductRequest;
import com.gwatcho.productservice.dto.ProductResponse;
import com.gwatcho.productservice.entity.ProductStatus;
import com.gwatcho.productservice.service.ProductService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;


    @Test
    void shouldCreateProduct() throws Exception {

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

        ProductResponse response =
                new ProductResponse(
                        1L,
                        "LAPTOP-001",
                        "Lenovo Laptop",
                        "Business laptop",
                        new BigDecimal("1200.00"),
                        "EUR",
                        25,
                        ProductStatus.ACTIVE,
                        "COMPUTERS",
                        0l,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        when(productService.createProduct(any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/products")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(
                        jsonPath("$.sku")
                                .value("LAPTOP-001")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Lenovo Laptop")
                )
                .andExpect(
                        jsonPath("$.price")
                                .value(1200.00)
                )
                .andExpect(
                        jsonPath("$.stockQuantity")
                                .value(25)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );
    }

    @Test
    void shouldRejectInvalidProduct() throws Exception {

        String invalidRequest = """
        {
          "sku": "",
          "name": "",
          "price": -100,
          "currency": "EU",
          "stockQuantity": -5,
          "category": ""
        }
        """;

        mockMvc.perform(
                        post("/api/products")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(invalidRequest)
                )
                .andExpect(
                        status().isBadRequest()
                );
    }

    @Test
    void shouldGetProduct() throws Exception {

        ProductResponse response =
                new ProductResponse(
                        1L,
                        "LAPTOP-001",
                        "Lenovo Laptop",
                        "Business laptop",
                        new BigDecimal("1200.00"),
                        "EUR",
                        25,
                        ProductStatus.ACTIVE,
                        "COMPUTERS",
                        0L,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        when(productService.getProduct(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/products/1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(
                        jsonPath("$.sku")
                                .value("LAPTOP-001")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Lenovo Laptop")
                );
    }
}
