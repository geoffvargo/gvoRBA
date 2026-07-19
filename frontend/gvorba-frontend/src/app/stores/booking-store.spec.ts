import { TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest';
import { of } from 'rxjs';

import { BookingStore } from './booking-store';
import { ApiService } from '../services/api.service';
import { MOCK_BOOKINGS } from '../testing/fixtures';

describe('BookingStore', () => {
	let store: BookingStore;
	let apiSpy: { getMyBookings: Mock };
	
	beforeEach(() => {
		apiSpy = {
			getMyBookings: vi.fn(),
		};
		
		TestBed.configureTestingModule({
			providers: [
				BookingStore,
				{ provide: ApiService, useValue: apiSpy },
			],
		});
		
		store = TestBed.inject(BookingStore);
	});
	
	it('should be created', () => {
		expect(store).toBeTruthy();
	});
	
	describe('loadMyBookings()', () => {
		it('should return an array of bookings', () => {
			apiSpy.getMyBookings.mockReturnValue(of(MOCK_BOOKINGS));
			
			store.loadMyBookings();
			
			expect(store.myBookings()).toEqual(MOCK_BOOKINGS);
			expect(store.isLoading()).toBe(false);
			expect(apiSpy.getMyBookings).toHaveBeenCalledTimes(1);
		});
	});
});
