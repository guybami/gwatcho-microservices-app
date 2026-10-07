import {DeliveryAddress} from "./checkout.model";

export interface OrderResponse {
  id: number;
  customerId: number;
  status: string;
  totalAmount: number;
  currency: string;
  deliveryAddress: DeliveryAddress;
  createdAt: string;
}
