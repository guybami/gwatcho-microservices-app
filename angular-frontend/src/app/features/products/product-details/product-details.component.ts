import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../core/models/product.model';
import { CartService } from '../../../core/services/cart.service';

@Component({
  selector: 'app-product-details',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink
  ],
  templateUrl: './product-details.component.html',
  styleUrl: './product-details.component.scss'
})
export class ProductDetailsComponent implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly productService = inject(ProductService);
  private readonly location = inject(Location);
  private readonly cartService = inject(CartService);

  product?: Product;
  quantity = 1;

  loading = false;
  errorMessage = '';

  ngOnInit(): void {

    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!id || id <= 0) {
      this.errorMessage = 'Invalid product ID.';
      return;
    }

    this.loadProduct(id);
  }

  private loadProduct(id: number): void {

    this.loading = true;
    this.errorMessage = '';

    this.productService.getProductById(id).subscribe({

      next: product => {
        this.product = product;
        this.loading = false;
      },

      error: error => {
        console.error('Error loading product:', error);

        this.errorMessage =
          'Unable to load the product. Please try again later.';
        this.loading = false;
      }

    });
  }

  goBack(): void {
    this.location.back();
  }

  addToCart(): void {

    if (!this.product) {
      return;
    }

    this.cartService.addToCart(
      this.product,
      this.quantity
    );

    console.log(
      `${this.quantity} x ${this.product.name} added to cart`
    );

  }

  increaseQuantity(): void {

    if (!this.product) {
      return;
    }

    if (this.quantity < this.product.stockQuantity) {
      this.quantity++;
    }
  }

  decreaseQuantity(): void {

    if (this.quantity > 1) {
      this.quantity--;
    }
  }
}
