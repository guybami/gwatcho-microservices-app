import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

import { Product } from '../models/product.model';
import { CartItem } from '../models/cart-item.model';

@Injectable({
  providedIn: 'root'
})
export class CartService {

  private readonly storageKey = 'gwatcho-cart';

  private readonly cartSubject =
    new BehaviorSubject<CartItem[]>(this.loadCart());

  readonly cart$ = this.cartSubject.asObservable();

  /**
   * Return the current cart.
   */
  getItems(): CartItem[] {
    return this.cartSubject.value;
  }

  /**
   * Add a product to the cart.
   */
  addToCart(product: Product, quantity = 1): void {

    if (product.stockQuantity <= 0) {
      return;
    }

    const items = [...this.cartSubject.value];

    const existingItem = items.find(
      item => item.product.id === product.id
    );

    if (existingItem) {

      const newQuantity =
        existingItem.quantity + quantity;

      existingItem.quantity =
        Math.min(newQuantity, product.stockQuantity);

    } else {

      items.push({
        product,
        quantity: Math.min(quantity, product.stockQuantity)
      });

    }

    this.updateCart(items);
  }

  /**
   * Increase quantity.
   */
  increaseQuantity(productId: number): void {

    const items = this.cartSubject.value;

    const item = items.find(
      item => item.product.id === productId
    );

    if (!item) {
      return;
    }

    if (item.quantity < item.product.stockQuantity) {
      item.quantity++;
      this.updateCart(items);
    }
  }

  /**
   * Decrease quantity.
   */
  decreaseQuantity(productId: number): void {

    const items = this.cartSubject.value;

    const item = items.find(
      item => item.product.id === productId
    );

    if (!item) {
      return;
    }

    if (item.quantity > 1) {
      item.quantity--;
      this.updateCart(items);
    }
  }

  /**
   * Remove product from cart.
   */
  remove(productId: number): void {

    const items = this.cartSubject.value.filter(
      item => item.product.id !== productId
    );

    this.updateCart(items);
  }

  /**
   * Empty the cart.
   */
  clear(): void {
    this.updateCart([]);
  }

  /**
   * Number of products in the cart.
   */
  getItemCount(): number {

    return this.cartSubject.value.reduce(
      (total, item) => total + item.quantity,
      0
    );
  }

  /**
   * Calculate total price.
   */
  getTotal(): number {

    return this.cartSubject.value.reduce(
      (total, item) =>
        total + item.product.price * item.quantity,
      0
    );
  }

  private updateCart(items: CartItem[]): void {

    this.cartSubject.next(items);

    localStorage.setItem(
      this.storageKey,
      JSON.stringify(items)
    );
  }

  private loadCart(): CartItem[] {

    const storedCart =
      localStorage.getItem(this.storageKey);

    if (!storedCart) {
      return [];
    }

    try {

      return JSON.parse(storedCart) as CartItem[];

    } catch (error) {

      console.error(
        'Unable to load cart:',
        error
      );

      return [];
    }
  }
}
