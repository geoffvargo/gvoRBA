import { inject, Injectable } from '@angular/core';
import { ApiService } from '../services/api.service';

@Injectable({
	providedIn: 'root',
})
export class BookingStore {
	private apiService = inject(ApiService);
}
