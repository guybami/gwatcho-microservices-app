package com.gwatcho.productservice.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateProductRequest(

        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull
        @Positive
        BigDecimal price,

        @NotBlank
        @Size(min = 3, max = 3)
        String currency,

        @NotBlank
        @Size(max = 100)
        String category
) {
}