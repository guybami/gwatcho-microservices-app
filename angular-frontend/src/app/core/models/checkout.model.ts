export interface DeliveryAddressRequest {
  street: string;
  postalCode: string;
  city: string;
  country: string;
}

export interface CheckoutItemRequest {
  productId: number;
  quantity: number;
}

export interface CheckoutRequest {
  customerId: number;
  currency: string;
  paymentMethod: string;
  deliveryAddress: DeliveryAddressRequest;
  items: CheckoutItemRequest[];
}


export interface DeliveryAddress {
  street: string;
  houseNumber: string;
  postalCode: string;
  city: string;
  country: string;
}
