import { Component, inject, OnInit, signal, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BookingStore } from '../stores/booking-store';
import { AuthStore } from '../stores/auth-store';

@Component({
	selector: 'app-booking-details',
	imports: [],
	templateUrl: './booking-details.component.html',
	styleUrl: './booking-details.component.css',
	encapsulation: ViewEncapsulation.None,
})
export class BookingDetailsComponent implements OnInit {
	private router = inject(Router);
	private route = inject(ActivatedRoute);
	
	protected bookingStore = inject(BookingStore);
	protected authStore = inject(AuthStore);
	protected bookingId = signal<number>(this.route.snapshot.params['id']);
	protected booking = this.bookingStore.currentBooking;
	protected isLoading = this.bookingStore.isLoading;
	protected isAdmin = this.authStore.isAdmin;
	
	readonly isCancelled = this.bookingStore.isCancelled;
	
	ngOnInit() {
		this.bookingStore.loadBooking(this.bookingId());
		console.log('currentBooking', this.booking());
	}
	
	onCancelBooking() {
		this.bookingStore.cancelBooking(this.bookingId());
		this.bookingStore.loadBookings();
		this.onBack();
	}
	
	onBack() {
		if (this.isAdmin()) {
			this.router.navigate(['admin/bookings'], {
				replaceUrl: true,
			}).then();
		} else {
			this.router.navigate(['..'], {
				relativeTo: this.route,
				replaceUrl: true,
			}).then();
		}
	}
}
