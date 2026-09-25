import { Component, inject, signal } from '@angular/core';
import { ProductService } from '../../core/services/product.service';
import { Product } from '../../core/models/product.model';
import {RouterLink} from "@angular/router";
import {CurrencyPipe} from "@angular/common";

@Component({
  selector: 'app-products',
  standalone: true,
  templateUrl: './products.component.html',
  imports: [
    RouterLink,
    CurrencyPipe
  ],
  styleUrl: './products.component.scss'
})
export class ProductsComponent {

  private readonly productService = inject(ProductService);
  readonly products = signal<Product[]>([]);
  readonly loading = signal(true);
  readonly hasError = signal(false);

  constructor() {
    this.loadProducts();
  }

  private loadProducts(): void {

    console.log('Starting product loading...');

    // Initial state
    this.loading.set(true);
    this.hasError.set(false);
    this.products.set([]);

    this.productService.getProducts().subscribe({

      next: (products: Product[]) => {
        console.log('Products received:', products.length);
        this.products.set(products);
        this.loading.set(false);
        this.hasError.set(false);
        //console.log('loading =', this.loading());
        //console.log('hasError =', this.hasError());
        //console.log('products =', this.products().length);
      },

      error: (error) => {
        console.error('Product request failed:', error);
        this.products.set([]);
        this.loading.set(false);
        this.hasError.set(true);
        console.log('loading =', this.loading());
        console.log('hasError =', this.hasError());
      },

      complete: () => {
        console.log('Product HTTP Observable completed');
      }
    });
  }
}
