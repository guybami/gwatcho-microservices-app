import {Component, inject, OnInit, signal} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { CartService } from '../../core/services/cart.service';
import {FormsModule} from "@angular/forms";

import { UserService } from '../../core/services/user.service';
import { CheckoutService } from '../../core/services/checkout.service';

import { Customer } from '../../core/models/customer.model';
import {OrderService} from "../../core/services/order.service";
import {CheckoutRequest} from "../../core/models/checkout.model";

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
  private readonly userService =  inject(UserService);
  readonly checkoutService =   inject(CheckoutService);
  private readonly orderService = inject(OrderService);

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
  readonly customer = this.checkoutService.customer;
  readonly loadingCustomer = this.checkoutService.loading;
  readonly customerError = this.checkoutService.error;

  /**
   * UI state signals
   */
  showCheckoutForm = signal(false);
  submitting = signal(false);
  hasError = signal(false);
  errorMessage = signal('');
  private customerId: number = -1;
  savingAddress = signal(false);

  /**
   * Delivery address
   */
  deliveryAddress: {
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    street: string;
    houseNumber: string;
    postalCode: string;
    city: string;
    country: string;
  } = {
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
    this.userService.getCurrentCustomer().subscribe({
      next: (customer: Customer) => {
        //console.log('Customer loaded:', customer);
        this.customerId = customer.id;
        this.checkoutService.setCustomer(customer);
        this.populateDeliveryAddress(customer);

        this.checkoutService.setLoading(false);
      },
      error: error => {
        console.error('Failed to load customer', error);
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
   * Save customer address
   */
  saveCustomerAddress(): void {

    this.submitting.set(true);
    this.hasError.set(false);
    this.errorMessage.set('');

    this.userService.updateCustomer({

      firstName: this.deliveryAddress.firstName,
      lastName: this.deliveryAddress.lastName,
      phone: this.deliveryAddress.phone,

      street: this.deliveryAddress.street,
      houseNumber: this.deliveryAddress.houseNumber,
      postalCode: this.deliveryAddress.postalCode,
      city: this.deliveryAddress.city,
      country: this.deliveryAddress.country

    }).subscribe({

      next: customer => {
        console.log('Customer address saved:', customer);
        this.checkoutService.setCustomer(customer);
        this.populateDeliveryAddress(customer);
        this.submitting.set(false);
      },

      error: error => {

        console.error(
          'Failed to save customer address:',
          error
        );

        this.submitting.set(false);

        this.hasError.set(true);

        this.errorMessage.set(
          error?.error?.message ??
          'Unable to save your delivery address.'
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
      firstName: customer.firstName ?? '',
      lastName: customer.lastName ?? '',

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
          this.checkoutService.setCustomer(customer);
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
    if (this.customerId === null) {
      this.hasError.set(true);
      this.errorMessage.set(
        'Customer information is not available.'
      );
      return;
    }

    if (!this.isDeliveryAddressValid()) {
      this.hasError.set(true);
      this.errorMessage.set('Please complete your delivery address.');
      return;
    }

    this.submitting.set(true);
    this.hasError.set(false);
    this.errorMessage.set('');

    // create order through OrderService
    const request: CheckoutRequest = {
      customerId: this.customerId,
      currency: 'EUR',
      paymentMethod: 'CREDIT_CARD',
      deliveryAddress: {
        street: this.deliveryAddress.street,
        postalCode: this.deliveryAddress.postalCode,
        city: this.deliveryAddress.city,
        country: this.deliveryAddress.country
      },

      items: this.items().map(item => ({
        productId: item.product.id,
        quantity: item.quantity
      }))
    };
    console.log('Creating order:', request);
    this.orderService.checkout(request).subscribe({
      next: order => {
        console.log('Order created successfully:', order);
        this.submitting.set(false);
        this.cartService.clear();
        // TODO: navigate to order confirmation
        alert('Order successfully placed...');
      },
      error: error => {
        console.error('Failed to create order:', error);
        this.submitting.set(false);
        this.hasError.set(true);
        this.errorMessage.set(
          error?.error?.message ??
          'Unable to create your order.'
        );
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
