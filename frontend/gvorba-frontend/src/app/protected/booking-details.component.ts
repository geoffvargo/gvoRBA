import { Component, inject, OnInit, signal, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BookingStore } from '../stores/booking-store';

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
	protected bookingId = signal<number>(this.route.snapshot.params['id']);
	protected booking = this.bookingStore.currentBooking;
	protected isLoading = this.bookingStore.isLoading;
	
	ngOnInit() {
		this.bookingStore.loadBooking(this.bookingId());
		console.log('currentBooking', this.booking());
	}
	
	onBack() {
		this.router.navigate(['..'], {
			relativeTo: this.route,
			replaceUrl: true,
		}).then();
	}
}
