import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Product } from '../../core/models/product.model';
import { CartService } from '../../core/services/cart.service';
import { ProductService } from '../../core/services/product.service';

@Component({
  selector: 'app-products',
  standalone: true,
  templateUrl: './products.component.html',
  imports: [RouterLink, CurrencyPipe],
  styleUrl: './products.component.scss'
})
export class ProductsComponent {
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);

  readonly products = signal<Product[]>([]);
  readonly loading = signal(true);
  readonly hasError = signal(false);

  constructor() {
    this.loadProducts();
  }

  addToCart(product: Product, event: Event): void {
    event.stopPropagation();
    this.cartService.addToCart(product);
  }

  private loadProducts(): void {
    this.loading.set(true);
    this.hasError.set(false);

    this.productService.getProducts().subscribe({
      next: (products: Product[]) => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Product request failed:', error);
        this.products.set([]);
        this.loading.set(false);
        this.hasError.set(true);
      }
    });
  }
}
