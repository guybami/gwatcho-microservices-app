package com.gwatcho.orderservice.entity;

import jakarta.persistence.Embeddable;

import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddress {

    private String street;

    private String postalCode;

    private String city;

    private String country;
}