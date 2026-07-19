import { inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { Booking } from '../models/booking.model';
import { BookingRequest} from '../models/booking-request.model';

@Injectable({
	providedIn: 'root',
})
export class BookingStore {
	private apiService = inject(ApiService);
	
	private _myBookings = signal<Booking[]>([]);
	private _conflictError = signal<string | null>(null);
	private _isLoading = signal<boolean>(false);
	
	readonly myBookings = this._myBookings.asReadonly();
	readonly conflictError = this._conflictError.asReadonly();
	readonly isLoading = this._isLoading.asReadonly();
	
	loadMyBookings() {
		this._isLoading.set(true);
		this.apiService.getMyBookings().subscribe({
			next: data => {
				this._myBookings.set(data);
				console.log(this.myBookings);
				this._isLoading.set(false);
			},
			error: err => {
				// TODO: check for and handle 409 error
				console.error(err);
				this._isLoading.set(false);
			}
		})
	}
	
	/** Create a new booking  */
	create(payload: BookingRequest) {
		this._isLoading.set(true);
		this.apiService.createBooking(payload).subscribe({
			next: data => {
				console.log(data);
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this._isLoading.set(false);
			}
		})
	}
	
	/** Cancels the `booking` with the supplied `id` */
	cancel(id: number) {
		this._isLoading.set(true);
		this.apiService.cancelBooking(id).subscribe({
			next: data => {
				console.log(data);
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this._isLoading.set(false);
			}
		})
	}
}
