package com.gwatcho.orderservice.service;

import com.gwatcho.orderservice.client.ProductCheckoutClient;
import com.gwatcho.orderservice.dto.CheckoutRequest;
import com.gwatcho.orderservice.dto.OrderResponse;
import com.gwatcho.orderservice.dto.ProductSnapshot;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final OrderService orderService;

    // Later this will be the ProductService client.
    private final ProductCheckoutClient productCheckoutClient;

    @Transactional
    public OrderResponse checkout(
            CheckoutRequest request) {

        List<ProductSnapshot> products =
                productCheckoutClient
                        .getProductsForCheckout(
                                request.items()
                        );

        return orderService.createOrder(
                request,
                products
        );
    }
}