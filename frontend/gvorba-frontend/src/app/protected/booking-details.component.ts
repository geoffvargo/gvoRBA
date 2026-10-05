import { Component, computed, inject, OnInit, signal, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { BookingStore } from '../stores/booking-store';
import { AuthStore } from '../stores/auth-store';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

@Component({
	selector: 'app-booking-details',
	imports: [
		MatProgressSpinner,
	],
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
	readonly attendeeNames = computed(() => {
		return this.booking().attendees.map(attendee => attendee.name).join(', ');
	});
	
	ngOnInit() {
		this.bookingStore.loadBooking(this.bookingId());
		console.log('currentBooking', this.booking());
		console.log(this.attendeeNames());
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
	
	onExportIcs() {
		console.log('exportIcs()');
		this.bookingStore.exportIcs(this.bookingId());
	}
	
	onEdit() {
		console.log('edit');
	}
	
	onBack() {
		this.location.back();
	}
}
