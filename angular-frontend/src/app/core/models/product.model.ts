export interface Product {
  id: number;
  sku: string;
  name: string;
  description: string;
  price: number;
  currency: string;
  stockQuantity: number;
  status: string;
  category: string;
  version: number;
  createdAt: string;
}

export interface ProductItem {
  productId: number;
  sku: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}
