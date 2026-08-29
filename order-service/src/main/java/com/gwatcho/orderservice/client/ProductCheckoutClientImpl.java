package com.gwatcho.orderservice.client;

import com.gwatcho.orderservice.client.dto.ProductResponse;
import com.gwatcho.orderservice.dto.CheckoutItemRequest;
import com.gwatcho.orderservice.dto.ProductSnapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCheckoutClientImpl     implements ProductCheckoutClient {

    private final RestClient productServiceRestClient;


    @Override
    public List<ProductSnapshot> getProductsForCheckout(
            List<CheckoutItemRequest> items) {

        return items.stream()
                .map(this::getProduct)
                .toList();
    }


    private ProductSnapshot getProduct(
            CheckoutItemRequest item) {

        Long productId = item.productId();
        log.debug(
                "Fetching product {} from ProductService",
                productId
        );

        try {

            ProductResponse productResponse =
                    productServiceRestClient
                            .get()
                            .uri(
                                    "/api/products/{id}",
                                    productId
                            )
                            .retrieve()

                            .onStatus(
                                    HttpStatusCode::is4xxClientError,
                                    (request, response) -> {

                                        log.error(
                                                "ProductService returned {} for product {}",
                                                response.getStatusCode(),
                                                productId
                                        );

                                        throw new IllegalStateException(
                                                "Product not found: "
                                                        + productId
                                        );
                                    }
                            )
                            .onStatus(
                                    HttpStatusCode::is5xxServerError,
                                    (request, response) -> {
                                        String errorBody;
                                        try {
                                            errorBody = new String(
                                                    response.getBody().readAllBytes()
                                            );
                                        } catch (Exception e) {
                                            errorBody = "<unable to read response body>";
                                        }

                                        log.error(
                                                "ProductService returned {} for product {}. Body: {}",
                                                response.getStatusCode(),
                                                productId,
                                                errorBody
                                        );

                                        throw new IllegalStateException(
                                                "ProductService returned "
                                                        + response.getStatusCode()
                                                        + " for product "
                                                        + productId
                                                        + ": "
                                                        + errorBody
                                        );
                                    }
                            )
                            .body(ProductResponse.class);

            if (productResponse == null) {

                log.error(
                        "ProductService returned empty response for product {}",
                        productId
                );

                throw new IllegalStateException(
                        "ProductService returned no product for: "
                                + productId
                );
            }

            log.debug(
                    "Product {} retrieved successfully: sku={}, price={}, currency={}, stock={}",
                    productResponse.id(),
                    productResponse.sku(),
                    productResponse.price(),
                    productResponse.currency(),
                    productResponse.stockQuantity()
            );


            return new ProductSnapshot(
                    productResponse.id(),
                    productResponse.sku(),
                    productResponse.name(),
                    productResponse.price(),
                    productResponse.currency(),
                    productResponse.stockQuantity()
            );

        } catch (IllegalStateException ex) {
            log.error(
                    "ProductService error for product {}: {}",
                    productId,
                    ex.getMessage()
            );
            throw ex;
        } catch (Exception ex) {
            log.error(
                    "Error retrieving product {} from ProductService",
                    productId,
                    ex
            );
            throw new IllegalStateException(
                    "Failed to retrieve product: "
                            + productId,
                    ex
            );
        }
    }
}