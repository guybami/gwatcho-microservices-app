import {ProductItem} from "./product.model";

export interface Order {
  id: number;
  customerId: number;
  status: string;
  totalAmount: number;
  currency: string;
  paymentMethod: string;

  street: string;
  city: string;
  postalCode: string;
  country: string;

  items: ProductItem[];

  createdAt: string;
}
