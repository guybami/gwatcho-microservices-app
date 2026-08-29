package com.gwatcho.orderservice.client;

import com.gwatcho.orderservice.dto.CheckoutItemRequest;
import com.gwatcho.orderservice.dto.ProductSnapshot;

import java.util.List;

public interface ProductCheckoutClient {

    List<ProductSnapshot> getProductsForCheckout(
            List<CheckoutItemRequest> items
    );
}