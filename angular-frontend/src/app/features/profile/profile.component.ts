import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-profile',
  imports: [FormsModule],
  templateUrl: './profile.component.html',
  standalone: true,
  styleUrl: './profile.component.scss'
})
export class ProfileComponent {

  firstName = '';
  lastName = '';
  email = '';
  phone = '';

  street = '';
  houseNumber = '';
  postalCode = '';
  city = '';
  country = 'Germany';

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
