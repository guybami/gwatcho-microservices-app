import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { Product } from '../../core/models/product.model';


@Component({
  selector: 'app-products',
  imports: [CommonModule, RouterLink],
  templateUrl: './products.component.html',
  standalone: true,
  styleUrl: './products.component.scss'
})
export class ProductsComponent implements OnInit {

  private readonly productService = inject(ProductService);

  products: Product[] = [];
  loading = false;
  errorMessage = '';
  error: string = '';

  ngOnInit(): void {
    console.log('ProductsComponent initialized');
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    this.productService.getProducts().subscribe({
      next: (products) => {
        console.log('Products received:', products);
        console.log('Number of products:', products.length);

        this.products = products;
        this.loading = false;
      },

      error: (error) => {
        console.error('Failed to load products:', error);

        this.error  = 'Unable to load products.';
        this.loading = false;
      }
    });
  }
}
