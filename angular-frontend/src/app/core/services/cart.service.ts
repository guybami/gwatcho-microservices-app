import { Injectable, computed, signal } from "@angular/core";

import { Product } from "../models/product.model";
import { CartItem } from "../models/cart-item.model";

@Injectable({
  providedIn: "root",
})
export class CartService {
  private readonly storageKey = "gwatcho-cart";

  /**
   * Single source of truth for the cart.
   */
  private readonly cartSignal = signal<CartItem[]>(this.loadCart());

  /**
   * Public read-only cart signal.
   */
  readonly cart = this.cartSignal.asReadonly();

  /**
   * Total number of items.
   */
  readonly itemCount = computed(() => this.cartSignal().reduce((total, item) => total + item.quantity, 0));

  /**
   * Total cart price.
   */
  readonly total = computed(() =>
    this.cartSignal().reduce((total, item) => total + item.product.price * item.quantity, 0),
  );

  /**
   * Return current cart items.
   */
  getItems(): CartItem[] {
    return this.cartSignal();
  }

  /**
   * Add product to cart.
   */
  addToCart(product: Product, quantity = 1): void {
    if (product.stockQuantity <= 0) {
      return;
    }

    this.cartSignal.update((items) => {
      const updatedItems = [...items];

      const existingItem = updatedItems.find((item) => item.product.id === product.id);

      if (existingItem) {
        const newQuantity = existingItem.quantity + quantity;

        existingItem.quantity = Math.min(newQuantity, product.stockQuantity);
      } else {
        updatedItems.push({
          product,
          quantity: Math.min(quantity, product.stockQuantity),
        });
      }

      this.persistCart(updatedItems);

      return updatedItems;
    });
  }

  /**
   * Increase product quantity.
   */
  increaseQuantity(productId: number): void {
    this.cartSignal.update((items) => {
      const updatedItems = items.map((item) => {
        if (item.product.id !== productId) {
          return item;
        }

        if (item.quantity >= item.product.stockQuantity) {
          return item;
        }

        return {
          ...item,
          quantity: item.quantity + 1,
        };
      });

      this.persistCart(updatedItems);

      return updatedItems;
    });
  }

  /**
   * Decrease product quantity.
   *
   * Minimum quantity is 1.
   */
  decreaseQuantity(productId: number): void {
    this.cartSignal.update((items) => {
      const updatedItems = items.map((item) => {
        if (item.product.id !== productId) {
          return item;
        }

        if (item.quantity <= 1) {
          return item;
        }

        return {
          ...item,
          quantity: item.quantity - 1,
        };
      });

      this.persistCart(updatedItems);

      return updatedItems;
    });
  }

  /**
   * Set an exact quantity.
   *
   * If quantity <= 0, the item is removed.
   */
  updateQuantity(productId: number, quantity: number): void {
    if (quantity <= 0) {
      this.removeItem(productId);
      return;
    }

    this.cartSignal.update((items) => {
      const updatedItems = items.map((item) => {
        if (item.product.id !== productId) {
          return item;
        }

        const newQuantity = Math.min(quantity, item.product.stockQuantity);

        return {
          ...item,
          quantity: newQuantity,
        };
      });

      this.persistCart(updatedItems);

      return updatedItems;
    });
  }

  /**
   * Remove product from cart.
   */
  removeItem(productId: number): void {
    const updatedItems = this.cartSignal().filter((item) => item.product.id !== productId);

    this.updateCart(updatedItems);
  }

  /**
   * Alias for removeItem().
   *
   * Keeps compatibility with existing code
   * that calls cartService.remove().
   */
  remove(productId: number): void {
    this.removeItem(productId);
  }

  /**
   * Empty the cart.
   */
  clear(): void {
    this.updateCart([]);
  }

  /**
   * Update cart and persist it.
   */
  private updateCart(items: CartItem[]): void {
    this.cartSignal.set(items);

    this.persistCart(items);
  }

  /**
   * Persist cart in localStorage.
   */
  private persistCart(items: CartItem[]): void {
    localStorage.setItem(this.storageKey, JSON.stringify(items));
  }

  /**
   * Load cart from localStorage.
   */
  private loadCart(): CartItem[] {
    const storedCart = localStorage.getItem(this.storageKey);

    if (!storedCart) {
      return [];
    }

    try {
      const parsed = JSON.parse(storedCart);

      if (!Array.isArray(parsed)) {
        return [];
      }

      return parsed as CartItem[];
    } catch (error) {
      console.error("Unable to load cart:", error);

      return [];
    }
  }
}
