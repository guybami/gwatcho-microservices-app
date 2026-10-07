import { Component, computed, inject, OnInit, signal } from "@angular/core";

import { CommonModule } from "@angular/common";
import { RouterLink } from "@angular/router";

import { OrderService } from "../../core/services/order.service";
import { UserService } from "../../core/services/user.service";
import { Order } from "../../core/models/order.model";
import { OrderDetailsComponent }  from './order-details/order-details.component';

@Component({
  selector: "app-orders",
  standalone: true,
  imports: [CommonModule, RouterLink, OrderDetailsComponent],
  templateUrl: "./orders.component.html",
  styleUrl: "./orders.component.scss",
})
export class OrdersComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly userService = inject(UserService);

  readonly orders = signal<Order[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly searchTerm = signal("");
  readonly selectedStatus = signal("ALL");

  readonly filteredOrders = computed(() => {
    const search = this.searchTerm().trim().toLowerCase();

    const status = this.selectedStatus();

    return this.orders().filter((order) => {
      const matchesSearch =
        !search ||
        order.id.toString().includes(search) ||
        order.status?.toLowerCase().includes(search);

      const matchesStatus = status === "ALL" || order.status === status;

      return matchesSearch && matchesStatus;
    });
  });

  readonly statuses = computed(() => {
    const values = this.orders()
      .map((order) => order.status)
      .filter(Boolean);

    return [...new Set(values)];
  });

  readonly selectedOrder = signal<Order | null>(null);

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading.set(true);
    this.error.set(null);

    this.userService.getCurrentCustomer().subscribe({
      next: (customer) => {
        this.orderService.getCustomerOrders(customer.id).subscribe({
          next: (orders) => {
            this.orders.set(orders);
            this.loading.set(false);
          },

          error: (error) => {
            console.error("Failed to load orders:", error);

            this.error.set("Unable to load your orders.");

            this.loading.set(false);
          },
        });
      },

      error: (error) => {
        console.error("Failed to load customer:", error);

        this.error.set("Unable to identify the current customer.");

        this.loading.set(false);
      },
    });
  }

  setSearchTerm(value: string): void {
    this.searchTerm.set(value);
  }

  setStatus(status: string): void {
    this.selectedStatus.set(status);
  }

  getStatusClass(status: string | undefined): string {
    switch (status?.toUpperCase()) {
      case "CREATED":
        return "status-created";

      case "PAID":
        return "status-paid";

      case "PROCESSING":
        return "status-processing";

      case "SHIPPED":
        return "status-shipped";

      case "DELIVERED":
        return "status-delivered";

      case "CANCELLED":
        return "status-cancelled";

      default:
        return "status-default";
    }
  }

  viewOrder(order: Order): void {
    this.selectedOrder.set(order);
  }

  closeOrderDetails(): void {
    this.selectedOrder.set(null);
  }

}
