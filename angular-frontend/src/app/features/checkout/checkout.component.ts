import {Component, inject, OnInit, signal} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { CartService } from '../../core/services/cart.service';
import {FormsModule} from "@angular/forms";

import { UserService } from '../../core/services/user.service';
import { CheckoutService } from '../../core/services/checkout.service';

import { Customer } from '../../core/models/customer.model';

@Component({
  selector: 'app-checkout',
  standalone: true,

  imports: [
    CommonModule,
    FormsModule
  ],

  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss'
})
export class CheckoutComponent implements OnInit {

  readonly cartService = inject(CartService);

  private readonly userService =
    inject(UserService);

  readonly checkoutService =
    inject(CheckoutService);

  /**
   * Cart signals
   */
  readonly items =
    this.cartService.cart;

  readonly itemCount =
    this.cartService.itemCount;

  readonly total =
    this.cartService.total;

  /**
   * Checkout state
   */
  readonly customer =
    this.checkoutService.customer;

  readonly loadingCustomer =
    this.checkoutService.loading;

  readonly customerError =
    this.checkoutService.error;

  /**
   * UI state
   */
  showCheckoutForm = signal(false);
  submitting = signal(false);
  hasError = signal(false);
  errorMessage = signal('');

  /**
   * Delivery address
   */
  deliveryAddress = {
    firstName: '',
    lastName: '',
    email: '',
    phone: '',

    street: '',
    houseNumber: '',
    postalCode: '',
    city: '',
    country: ''
  };

  ngOnInit(): void {

    this.loadCustomer();
  }

  /**
   * Load authenticated customer from User Service.
   */
  private loadCustomer(): void {

    this.checkoutService.setLoading(true);

    this.userService
      .getCurrentCustomer()
      .subscribe({

        next: (customer: Customer) => {

          this.checkoutService.setCustomer(customer);

          this.populateDeliveryAddress(customer);

          this.checkoutService.setLoading(false);
        },

        error: (error) => {

          console.error(
            'Failed to load customer',
            error
          );

          this.checkoutService.setLoading(false);

          this.checkoutService.setError(
            'Unable to load your customer profile.'
          );

          this.hasError.set(true);
          this.errorMessage.set(
            'Unable to load your customer profile.'
          );
        }
      });
  }

  /**
   * Copy customer information into the
   * checkout delivery address.
   */
  private populateDeliveryAddress(
    customer: Customer
  ): void {

    this.deliveryAddress = {

      firstName:
        customer.firstName ?? '',

      lastName:
        customer.lastName ?? '',

      email:
        customer.email ?? '',

      phone:
        customer.phone ?? '',

      street:
        customer.street ?? '',

      houseNumber:
        customer.houseNumber ?? '',

      postalCode:
        customer.postalCode ?? '',

      city:
        customer.city ?? '',

      country:
        customer.country ?? ''
    };
  }

  /**
   * Continue from cart to checkout.
   */
  continueToCheckout(): void {

    if (this.itemCount() === 0) {
      return;
    }

    this.showCheckoutForm.set(true);;

    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  }

  /**
   * Update customer/address.
   */
  updateCustomer(): void {

    this.submitting.set(true);

    this.hasError.set(false);

    this.userService
      .updateCustomer({

        firstName:
        this.deliveryAddress.firstName,

        lastName:
        this.deliveryAddress.lastName,

        phone:
        this.deliveryAddress.phone,

        street:
        this.deliveryAddress.street,

        houseNumber:
        this.deliveryAddress.houseNumber,

        postalCode:
        this.deliveryAddress.postalCode,

        city:
        this.deliveryAddress.city,

        country:
        this.deliveryAddress.country
      })
      .subscribe({

        next: (customer) => {

          this.checkoutService
            .setCustomer(customer);

          this.populateDeliveryAddress(
            customer
          );

          this.submitting.set(false);;
        },

        error: (error) => {

          console.error(
            'Customer update failed',
            error
          );

          this.submitting.set(false);
          this.hasError.set(true);;
          this.errorMessage.set('Unable to update your delivery address.');
        }
      });
  }

  /**
   * Remove item from cart.
   */
  removeItem(productId: number): void {
    this.cartService.removeItem(productId);
  }

  /**
   * Change quantity.
   */
  updateQuantity(
    productId: number,
    quantity: number
  ): void {

    if (quantity <= 0) {
      this.cartService.removeItem(productId);
      return;
    }

    this.cartService.updateQuantity(
      productId,
      quantity
    );
  }

  /**
   * Place order.
   *
   * We will connect this to OrderService next.
   */
  placeOrder(): void {

    if (this.itemCount() === 0) {
      return;
    }

    if (!this.isDeliveryAddressValid()) {
      this.hasError.set(true);
      this.errorMessage.set('Please complete your delivery address.');
      return;
    }

    this.submitting.set(true);

    this.hasError.set(false);

    this.userService
      .updateCustomer({

        firstName:
        this.deliveryAddress.firstName,

        lastName:
        this.deliveryAddress.lastName,

        phone:
        this.deliveryAddress.phone,

        street:
        this.deliveryAddress.street,

        houseNumber:
        this.deliveryAddress.houseNumber,

        postalCode:
        this.deliveryAddress.postalCode,

        city:
        this.deliveryAddress.city,

        country:
        this.deliveryAddress.country
      })
      .subscribe({

        next: (customer) => {

          this.checkoutService
            .setCustomer(customer);

          this.submitting.set(false);;

          // Next step:
          // create order through OrderService
          console.log(
            'Customer saved. Ready to create order.',
            customer
          );
        },

        error: (error) => {
          console.error(
            'Failed to save customer',
            error
          );

          this.submitting.set(false);;

          this.hasError.set(true);;

          this.errorMessage.set( 'Could not save your delivery address.');
        }
      });
  }

  /**
   * Validate delivery address.
   */
  private isDeliveryAddressValid(): boolean {

    const address = this.deliveryAddress;

    return !!(
      address.firstName &&
      address.lastName &&
      address.email &&
      address.street &&
      address.houseNumber &&
      address.postalCode &&
      address.city &&
      address.country
    );
  }
}
