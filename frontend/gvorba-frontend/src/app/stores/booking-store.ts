import { inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { Booking } from '../models/booking.model';
import { BookingRequest } from '../models/booking-request.model';
import { BookingResponse } from '../models/booking.response';

@Injectable({
	providedIn: 'root',
})
export class BookingStore {
	private apiService = inject(ApiService);
	
	private _myBookings = signal<Booking[]>([]);
	private _bookings = signal<Booking[]>([]);
	private _conflictError = signal<string | null>(null);
	private _isLoading = signal<boolean>(false);
	private _isCancelled = signal<boolean>(false);
	
	readonly myBookings = this._myBookings.asReadonly();
	readonly bookings = this._bookings.asReadonly();
	readonly conflictError = this._conflictError.asReadonly();
	readonly isLoading = this._isLoading.asReadonly();
	readonly isCancelled = this._isCancelled.asReadonly();
	
	currentBooking = signal<BookingResponse>(new BookingResponse());
	
	loadBooking(id: number) {
		this._isLoading.set(true);
		this.apiService.getBooking(id).pipe(
			// delay(5000)
		).subscribe({
			next: booking => {
				this.currentBooking.set(booking);
				this._isCancelled.set(this.currentBooking().status === 'CANCELLED');
				console.log(booking);
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this._isLoading.set(false);
			},
		});
	}
	
	loadMyBookings() {
		this._isLoading.set(true);
		this.apiService.getMyBookings().subscribe({
			next: data => {
				this._myBookings.set(data);
				console.log(this.myBookings());
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this._isLoading.set(false);
			},
		});
	}
	
	loadBookings() {
		this._isLoading.set(true);
		this.apiService.getBookings().subscribe({
			next: data => {
				this._bookings.set(data);
				console.log(this.bookings());
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this._isLoading.set(false);
			},
		});
	}
	
	/** Create a new booking  */
	create(payload: BookingRequest) {
		this._isLoading.set(true);
		return this.apiService.createBooking(payload).subscribe({
			next: data => {
				console.log(data);
				this.loadBookings();
				this.loadMyBookings();
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this._conflictError.set(err);
				this._isLoading.set(false);
			},
		});
	}
	
	/** Cancels the `booking` with the supplied `id` */
	cancelBooking(id: number) {
		this._isLoading.set(true);
		this.apiService.cancelBooking(id).subscribe({
			next: data => {
				console.log(data);
				this.loadBookings();
				this.loadMyBookings();
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this._isLoading.set(false);
			},
		});
	}
	
	uncancelBooking(id: number) {
		this._isLoading.set(true);
		this.apiService.uncancelBooking(id).subscribe({
			next: data => {
				console.log(data);
				this.loadBookings();
				this.loadMyBookings();
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this._isLoading.set(false);
			},
		});
	}
}
