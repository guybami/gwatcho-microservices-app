import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Order } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-details',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-details.component.html',
  styleUrl: './order-details.component.scss'
})
export class OrderDetailsComponent {

  @Input({ required: true })
  order!: Order;

  @Output()
  closed = new EventEmitter<void>();

  close(): void {
    this.closed.emit();
  }

  getStatusClass(status: string | undefined): string {

    switch (status?.toUpperCase()) {

      case 'CREATED':
        return 'status-created';

      case 'PAID':
        return 'status-paid';

      case 'PROCESSING':
        return 'status-processing';

      case 'SHIPPED':
        return 'status-shipped';

      case 'DELIVERED':
        return 'status-delivered';

      case 'CANCELLED':
        return 'status-cancelled';

      default:
        return 'status-default';
    }
  }
}
