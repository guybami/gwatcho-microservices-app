import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { Product } from '../../core/models/product.model';


@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './products.component.html',
  styleUrl: './products.component.scss'
})
export class ProductsComponent implements OnInit {

  private readonly productService = inject(ProductService);

  products: Product[] = [];
  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {

    this.loading = true;
    this.errorMessage = '';

    this.productService.getProducts().subscribe({
      next: products => {
        this.products = products;
        this.loading = false;
      },

      error: error => {
        console.error('Error loading products:', error);
        this.errorMessage =
          'Unable to load products. Please try again later.';
        this.loading = false;
      }
    });
  }
}
