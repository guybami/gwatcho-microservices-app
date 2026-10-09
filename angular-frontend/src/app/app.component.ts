import { Component, inject } from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';

import { AuthService } from './core/auth/auth.service';
import { CartService } from './core/services/cart.service';

@Component({
  selector: 'app-root',
  imports: [
    RouterLink,
    RouterOutlet,
    RouterLinkActive
  ],
  templateUrl: './app.component.html',
  standalone: true,
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'gwatcho-microservices-app';
  readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);
  readonly cartItemCount = this.cartService.itemCount;

  constructor() {
    console.log('Authenticated:', this.authService.isAuthenticated());
    console.log('Username:', this.authService.getUsername());
    console.log('Email:', this.authService.getEmail());
    //console.log('Token:', this.authService.getToken());
  }

  async login(): Promise<void> {
    await this.authService.login();
  }

  async logout(): Promise<void> {
    await this.authService.logout();
  }


}
