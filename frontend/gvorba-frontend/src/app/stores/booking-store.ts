import { inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { Booking } from '../models/booking.model';
import { BookingRequest } from '../models/booking-request.model';
import { BookingResponse } from '../models/booking.response';
import { saveBlob } from '../utils/save-blob';
import { catchError, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { HotToastService } from '@ngxpert/hot-toast';

@Injectable({
	providedIn: 'root',
})
export class BookingStore {
	private apiService = inject(ApiService);
	private toast = inject(HotToastService);
	
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
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
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
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
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
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
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
				this.toast.success('Booking successfully created!', {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this._conflictError.set(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
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
				this.toast.success('Booking canceled successfully.', {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
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
				this.toast.success('Booking uncanceled successfully.', {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
		});
	}
	
	exportIcs(bookingId: number) {
		this._isLoading.set(true);
		const filename = `booking-${bookingId}.ics`;
		console.log(filename);
		this.apiService.exportIcs(bookingId).pipe(
			catchError((error: HttpErrorResponse) => {
				console.error('Booking fetch failed:', error.status, error.message);
				// Return a safe value or re-throw a user-friendly error
				return throwError(() => new Error('Failed to load bookings. Please try again later.'));
			}),
		).subscribe({
			next: data => {
				console.log(data.toString());
				saveBlob(data, filename);
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
		});
	}
	
	updateBooking(id: number, payload: BookingRequest) {
		this._isLoading.set(true);
		return this.apiService.updateBooking(id,payload).subscribe({
			next: data => {
				console.log(data);
				this._isLoading.set(false);
				// Reload only after the server has applied the update, so the details page shows the new values.
				this.loadBooking(id);
				this.loadBookings();
				this.toast.success('Booking updated successfully.', {
					position: 'bottom-center',
					dismissible: true,
				});
			},
			error: err => {
				console.error(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
		});
	}
}
