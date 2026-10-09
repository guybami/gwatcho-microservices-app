import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { OrdersComponent } from './orders.component';

describe('OrdersComponent', () => {
  let component: OrdersComponent;
  let fixture: ComponentFixture<OrdersComponent>;
  let httpMock: HttpTestingController;

  const customer = {
    id: 1,
    firstName: 'Test',
    lastName: 'Customer',
    email: 'test@example.com',
    phone: '+49123456789',
    street: 'Test Street',
    houseNumber: '1',
    postalCode: '74072',
    city: 'Heilbronn',
    country: 'Germany',
  };

  const orders = [
    {
      id: 1,
      customerId: 1,
      status: 'CREATED',
      totalAmount: 99.99,
      currency: 'EUR',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrdersComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OrdersComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    fixture.detectChanges();

    const customerRequest = httpMock.expectOne('/api/users/me');
    customerRequest.flush(customer);

    const ordersRequest = httpMock.expectOne(
      (request) =>
        request.url.includes('/api/orders') ||
        request.url.includes('/orders'),
    );

    ordersRequest.flush(orders);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
