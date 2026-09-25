import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink
  ],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss'
})
export class CartComponent {

  private readonly cartService = inject(CartService);

  /**
   * Reactive cart items.
   */
  readonly items = this.cartService.cart;

  /**
   * Reactive total price.
   */
  readonly total = this.cartService.total;

  /**
   * Reactive item count.
   */
  readonly itemCount = this.cartService.itemCount;


  increase(productId: number): void {
    this.cartService.increaseQuantity(productId);
  }


  decrease(productId: number): void {
    this.cartService.decreaseQuantity(productId);
  }


  remove(productId: number): void {
    this.cartService.remove(productId);
  }


  clear(): void {
    this.cartService.clear();
  }
}
