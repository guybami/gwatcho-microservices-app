
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CheckoutRequest
} from '../models/checkout.model';


@Injectable({
  providedIn: 'root'
})
export class OrderService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl =
    'http://localhost:8082/order-service/orders';

  checkout(request: CheckoutRequest): Observable<unknown> {
    return this.http.post(
      `${this.apiUrl}/checkout`,
      request
    );
  }
}
