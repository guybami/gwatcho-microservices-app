import {Component, OnInit, inject, signal} from '@angular/core';
import {CommonModule, CurrencyPipe, Location} from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../core/models/product.model';
import { CartService } from '../../../core/services/cart.service';


@Component({
  selector: 'app-product-details',
  standalone: true,
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './product-details.component.html',
  styleUrl: './product-details.component.scss'
})
export class ProductDetailsComponent implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly productService = inject(ProductService);
  private readonly location = inject(Location);
  private readonly cartService = inject(CartService);

  readonly product = signal<Product | null>(null);
  readonly loading = signal(true);
  readonly hasError = signal(false);

  readonly cart = this.cartService.cart;
  readonly itemCount = this.cartService.itemCount;
  readonly total = this.cartService.total;

  quantity = signal(1);
  errorMessage = signal('');

  constructor() {
    this.loadProduct();
  }

  ngOnInit(): void {
  }

  private loadProduct(): void {

    this.loading.set(true);
    this.hasError.set(false);
    this.product.set(null);

    const idParam = this.route.snapshot.paramMap.get('id');

    if (!idParam) {
      console.error('Product ID is missing');

      this.loading.set(false);
      this.hasError.set(true);

      return;
    }

    const productId = Number(idParam);

    if (Number.isNaN(productId)) {
      console.error('Invalid product ID:', idParam);

      this.loading.set(false);
      this.hasError.set(true);

      return;
    }

    console.log('Loading product:', productId);

    this.productService.getProductById(productId).subscribe({

      next: (product: Product) => {

        console.log('Product received:', product);

        this.product.set(product);

        this.loading.set(false);
        this.hasError.set(false);

        console.log('loading =', this.loading());
        console.log('hasError =', this.hasError());
        console.log('product =', this.product());
      },

      error: (error) => {

        console.error(
          `Failed to load product ${productId}:`,
          error
        );

        this.product.set(null);

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

  goBack(): void {
    this.location.back();
  }

  addToCart(): void {
    const product = this.product();
    if (!product) {
      return;
    }

    this.cartService.addToCart(
      product,
      this.quantity()
    );

    console.log(
      `Added ${this.quantity()} x ${product.name} to cart`
    );
  }

  increaseQuantity(): void {
    const product = this.product();
    if (!product) {
      return;
    }
    if (this.quantity() < product.stockQuantity) {
      this.quantity.set(this.quantity() + 1);
    }
  }

  decreaseQuantity(): void {

    if (this.quantity() > 1) {
      this.quantity.set(this.quantity() - 1);
    }
  }


}
