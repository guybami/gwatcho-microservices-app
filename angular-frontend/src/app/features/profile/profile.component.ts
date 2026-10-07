import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../core/services/user.service';
import {CustomerRequest} from "../../core/models/customer.model";

@Component({
  selector: 'app-profile',
  imports: [FormsModule],
  templateUrl: './profile.component.html',
  standalone: true,
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {

  // Loading state
  loading = true;
  saving = false;
  errorMessage = '';

  // Personal information
  firstName = '';
  lastName = '';
  email = '';
  phone = '';

  // Delivery address
  street = '';
  houseNumber = '';
  postalCode = '';
  city = '';
  country = 'Germany';

  constructor(
    private readonly userService: UserService
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
  }

  /**
   * Load the authenticated customer profile.
   */
  private loadCurrentUser(): void {
    this.loading = true;
    this.errorMessage = '';

    this.userService.getCurrentCustomer().subscribe({
      next: (customer) => {
        console.log('Profile loaded:', customer);
        let customerRequest: CustomerRequest = this.userService.getCustomerRequest(customer);
        this.firstName = customerRequest.firstName ?? '';
        this.lastName = customerRequest.lastName ?? '';
        this.email = customerRequest.email ?? '';
        this.phone = customerRequest.phone ?? '';
        if (customerRequest.deliveryAddress) {
          this.street = customerRequest.deliveryAddress.street ?? '';
          this.houseNumber = customerRequest.deliveryAddress.houseNumber ?? '';
          this.postalCode = customerRequest.deliveryAddress.postalCode ?? '';
          this.city = customerRequest.deliveryAddress.city ?? '';
          this.country = customerRequest.deliveryAddress.country ?? 'Germany';
        }

        this.loading = false;
      },
      error: (error) => {
        console.error('Failed to load profile:', error);

        this.errorMessage =
          'Unable to load your profile. Please try again.';

        this.loading = false;
      }
    });
  }

  saveProfile(): void {
    console.log('Profile:', {
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      phone: this.phone,
      address: {
        street: this.street,
        houseNumber: this.houseNumber,
        postalCode: this.postalCode,
        city: this.city,
        country: this.country
      }
    });
  }
}
