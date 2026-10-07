import { Component, inject, OnInit, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RouterLink } from "@angular/router";
import { FormsModule } from "@angular/forms";

import { CartService } from "../../core/services/cart.service";
import { UserService } from "../../core/services/user.service";
import { CheckoutService } from "../../core/services/checkout.service";
import { Customer } from "../../core/models/customer.model";
import { OrderService } from "../../core/services/order.service";
import { CheckoutRequest } from "../../core/models/checkout.model";

interface CreatedOrder {
  id: number;
  status?: string;
  totalAmount?: number;
  currency?: string;
}

@Component({
  selector: "app-checkout",
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: "./checkout.component.html",
  styleUrl: "./checkout.component.scss",
})
export class CheckoutComponent implements OnInit {
  readonly cartService = inject(CartService);
  private readonly userService = inject(UserService);
  readonly checkoutService = inject(CheckoutService);
  private readonly orderService = inject(OrderService);

  readonly items = this.cartService.cart;
  readonly itemCount = this.cartService.itemCount;
  readonly total = this.cartService.total;

  readonly customer = this.checkoutService.customer;
  readonly loadingCustomer = this.checkoutService.loading;
  readonly customerError = this.checkoutService.error;

  /** Current interactive checkout step: 1..4. */
  readonly currentStep = signal(1);

  readonly submitting = signal(false);
  readonly hasError = signal(false);
  readonly errorMessage = signal("");
  readonly createdOrder = signal<CreatedOrder | null>(null);

  private customerId = -1;

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
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    street: "",
    houseNumber: "",
    postalCode: "",
    city: "",
    country: "",
  };

  readonly paymentMethod = "CREDIT_CARD";

  ngOnInit(): void {
    this.loadCustomer();
  }

  private loadCustomer(): void {
    this.checkoutService.setLoading(true);

    this.userService.getCurrentCustomer().subscribe({
      next: (customer: Customer) => {
        this.customerId = customer.id;
        this.checkoutService.setCustomer(customer);
        this.populateDeliveryAddress(customer);
        this.checkoutService.setLoading(false);
      },
      error: (error) => {
        console.error("Failed to load customer", error);
        this.checkoutService.setLoading(false);
        this.checkoutService.setError(
          "Unable to load your customer profile.",
        );
        this.hasError.set(true);
        this.errorMessage.set("Unable to load your customer profile.");
      },
    });
  }

  /**
   * Step 1 -> Step 2.
   */
  continueToCheckout(): void {
    if (this.itemCount() === 0) {
      return;
    }

    this.clearError();
    this.currentStep.set(2);
    this.scrollToTop();
  }

  /**
   * Navigate to a previously completed step.
   * Future steps cannot be selected directly.
   */
  goToStep(step: number): void {
    if (step >= 1 && step <= this.currentStep()) {
      this.clearError();
      this.currentStep.set(step);
      this.scrollToTop();
    }
  }

  /**
   * Step 2: save the address and move to the summary.
   */
  continueToSummary(): void {
    if (!this.isDeliveryAddressValid()) {
      this.hasError.set(true);
      this.errorMessage.set("Please complete your delivery address.");
      return;
    }

    this.updateCustomer(true);
  }

  /**
   * Save the address without advancing the stepper.
   */
  saveCustomerAddress(): void {
    if (!this.isDeliveryAddressValid()) {
      this.hasError.set(true);
      this.errorMessage.set("Please complete your delivery address.");
      return;
    }

    this.updateCustomer(false);
  }

  /**
   * Persist customer/address changes.
   */
  private updateCustomer(advanceToSummary: boolean): void {
    this.submitting.set(true);
    this.clearError();

    this.userService
      .updateCustomer({
        firstName: this.deliveryAddress.firstName,
        lastName: this.deliveryAddress.lastName,
        phone: this.deliveryAddress.phone,
        street: this.deliveryAddress.street,
        houseNumber: this.deliveryAddress.houseNumber,
        postalCode: this.deliveryAddress.postalCode,
        city: this.deliveryAddress.city,
        country: this.deliveryAddress.country,
      })
      .subscribe({
        next: (customer) => {
          console.log("Customer address saved:", customer);

          this.customerId = customer.id;
          this.checkoutService.setCustomer(customer);
          this.populateDeliveryAddress(customer);
          this.submitting.set(false);

          if (advanceToSummary) {
            this.currentStep.set(3);
            this.scrollToTop();
          }
        },
        error: (error) => {
          console.error("Customer update failed", error);

          this.submitting.set(false);
          this.hasError.set(true);
          this.errorMessage.set(
            error?.error?.message ??
            "Unable to save your delivery address.",
          );
        },
      });
  }

  private populateDeliveryAddress(customer: Customer): void {
    this.deliveryAddress = {
      firstName: customer.firstName ?? "",
      lastName: customer.lastName ?? "",
      email: customer.email ?? "",
      phone: customer.phone ?? "",
      street: customer.street ?? "",
      houseNumber: customer.houseNumber ?? "",
      postalCode: customer.postalCode ?? "",
      city: customer.city ?? "",
      country: customer.country ?? "",
    };
  }

  /**
   * Step 3 -> Step 4.
   */
  placeOrder(): void {
    if (this.itemCount() === 0) {
      return;
    }

    if (this.customerId < 0) {
      this.hasError.set(true);
      this.errorMessage.set("Customer information is not available.");
      return;
    }

    if (!this.isDeliveryAddressValid()) {
      this.hasError.set(true);
      this.errorMessage.set("Please complete your delivery address.");
      this.currentStep.set(2);
      return;
    }

    this.submitting.set(true);
    this.clearError();

    const request: CheckoutRequest = {
      customerId: this.customerId,
      currency: "EUR",
      paymentMethod: this.paymentMethod,
      deliveryAddress: {
        street: this.deliveryAddress.street,
        postalCode: this.deliveryAddress.postalCode,
        city: this.deliveryAddress.city,
        country: this.deliveryAddress.country,
      },
      items: this.items().map((item) => ({
        productId: item.product.id,
        quantity: item.quantity,
      })),
    };

    console.log("Creating order:", request);

    this.orderService.checkout(request).subscribe({
      next: (order) => {
        console.log("Order created successfully:", order);

        this.createdOrder.set({
          id: order.id,
          status: order.status,
          totalAmount: order.totalAmount,
          currency: order.currency,
        });

        this.submitting.set(false);
        this.cartService.clear();
        this.currentStep.set(4);
        this.scrollToTop();
      },
      error: (error) => {
        console.error("Failed to create order:", error);

        this.submitting.set(false);
        this.hasError.set(true);
        this.errorMessage.set(
          error?.error?.message ?? "Unable to create your order.",
        );
      },
    });
  }

  removeItem(productId: number): void {
    this.cartService.removeItem(productId);

    if (this.itemCount() === 0) {
      this.currentStep.set(1);
    }
  }

  updateQuantity(productId: number, quantity: number): void {
    if (quantity <= 0) {
      this.removeItem(productId);
      return;
    }

    this.cartService.updateQuantity(productId, quantity);
  }

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

  private clearError(): void {
    this.hasError.set(false);
    this.errorMessage.set("");
  }

  private scrollToTop(): void {
    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  }
}
