import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';

import { Customer } from '../models/customer.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8081/user-service/api/users';

  private customer$?: Observable<Customer>;

  /**
   * Returns the currently authenticated customer.
   *
   * The request is cached during the lifetime of the service
   * so multiple components do not trigger multiple HTTP calls.
   */
  getCurrentCustomer(): Observable<Customer> {

    if (!this.customer$) {

      this.customer$ = this.http
        .get<Customer>(`${this.apiUrl}/me`)
        .pipe(
          shareReplay(1)
        );
    }

    return this.customer$;
  }

  /**
   * Update the currently authenticated customer.
   */
  updateCustomer(
    customer: Partial<Customer>
  ): Observable<Customer> {

    return this.http.put<Customer>(
      `${this.apiUrl}/me`,
      customer
    );
  }

  /**
   * Clear cached customer.
   *
   * Useful after logout/login with another Keycloak user.
   */
  clearCustomer(): void {
    this.customer$ = undefined;
  }
}
