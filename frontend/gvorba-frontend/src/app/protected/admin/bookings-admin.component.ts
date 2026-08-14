import { Component, inject } from '@angular/core';
import { BookingStore } from '../../stores/booking-store';

@Component({
	selector: 'app-bookings-admin',
	imports: [],
	templateUrl: './bookings-admin.component.html',
	styleUrl: './bookings-admin.component.css',
})
export class BookingsAdminComponent {
	protected bookingStore = inject(BookingStore);
}
