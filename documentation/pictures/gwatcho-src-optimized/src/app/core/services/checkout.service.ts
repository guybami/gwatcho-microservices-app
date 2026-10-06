import { Injectable, computed, inject, signal } from '@angular/core';
import { Customer } from '../models/customer.model';

@Injectable({
  providedIn: 'root'
})
export class CheckoutService {

  private readonly customerState = signal<Customer | null>(null);

  private readonly loadingState = signal(false);

  private readonly errorState =
    signal<string | null>(null);

  // Public readonly signals
  readonly customer =
    this.customerState.asReadonly();

  readonly loading =
    this.loadingState.asReadonly();

  readonly error =
    this.errorState.asReadonly();

  /**
   * Customer full name.
   */
  readonly fullName = computed(() => {
    const customer = this.customer();
    if (!customer) {
      return '';
    }

    return `${customer.firstName} ${customer.lastName}`.trim();
  });

  /**
   * Whether a complete delivery address exists.
   */
  readonly hasDeliveryAddress = computed(() => {

    const customer = this.customer();
    if (!customer) {
      return false;
    }

    return !!(
      customer.street &&
      customer.houseNumber &&
      customer.postalCode &&
      customer.city &&
      customer.country
    );
  });

  setCustomer(customer: Customer): void {

    this.customerState.set(customer);

    this.errorState.set(null);
  }

  setLoading(value: boolean): void {
    this.loadingState.set(value);
  }

  setError(message: string | null): void {
    this.errorState.set(message);
  }

  clear(): void {
    this.customerState.set(null);
    this.loadingState.set(false);
    this.errorState.set(null);
  }
}
