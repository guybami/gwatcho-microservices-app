
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CheckoutRequest
} from '../models/checkout.model';
import {Order} from "../models/order.model";


@Injectable({
  providedIn: 'root'
})
export class OrderService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'api/order-service/orders';

  checkout(request: CheckoutRequest): Observable<Order> {
    return this.http.post<Order>(
      `${this.apiUrl}/checkout`,
      request
    );
  }

  getCustomerOrders(customerId: number): Observable<Order[]> {
    return this.http.get<Order[]>(
      `${this.apiUrl}/customer/${customerId}`
    );
  }

  cancelOrder(id: number): Observable<Order> {
    return this.http.delete<Order>(
      `${this.apiUrl}/${id}`
    );
  }


}
