import { Component, computed, inject, OnInit, signal, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
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
	private location = inject(Location);
	
	protected bookingStore = inject(BookingStore);
	protected authStore = inject(AuthStore);
	protected bookingId = signal<number>(this.route.snapshot.params['id']);
	protected booking = this.bookingStore.currentBooking;
	protected isLoading = this.bookingStore.isLoading;
	protected isAdmin = this.authStore.isAdmin;
	
	readonly currUser = this.authStore.user;
	readonly isCancelled = this.bookingStore.isCancelled;
	readonly isOwner = computed(() =>
		this.isAdmin() || this.currUser()?.id === this.booking().userId.id);
	
	ngOnInit() {
		this.bookingStore.loadBooking(this.bookingId());
		console.log('currentBooking', this.booking());
	}
	
	onCancelBooking() {
		this.bookingStore.cancelBooking(this.bookingId());
		this.bookingStore.loadBookings();
		this.onBack();
	}
	
	onUncancelBooking() {
		this.bookingStore.uncancelBooking(this.bookingId());
		this.onBack();
	}
	
	onBack() {
		this.location.back();
		// if (this.isAdmin()) {
		// 	this.router.navigate(['admin/bookings'], {
		// 		replaceUrl: true,
		// 	}).then();
		// } else {
		// 	this.router.navigate(['..'], {
		// 		relativeTo: this.route,
		// 		replaceUrl: true,
		// 	}).then();
		// }
	}
}
