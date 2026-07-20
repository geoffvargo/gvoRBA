import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, type Mock, vi } from 'vitest';
import { of, Subject, throwError } from 'rxjs';

import { BookingStore } from './booking-store';
import { ApiService } from '../services/api.service';
import { MOCK_BOOKINGS } from '../testing/fixtures';
import { BookingRequest } from '../models/booking-request.model';
import { HttpErrorResponse } from '@angular/common/http';

describe('BookingStore', () => {
	let store: BookingStore;
	let apiSpy: {
		getMyBookings: Mock,
		createBooking: Mock,
		cancelBooking: Mock,
	};
	
	beforeEach(() => {
		apiSpy = {
			getMyBookings: vi.fn(),
			createBooking: vi.fn(),
			cancelBooking: vi.fn(),
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
	
	describe('createBooking()', () => {
		const payload = {} as BookingRequest;
		
		it('should forward the payload to ApiServcice.createBooking', () => {
			apiSpy.createBooking.mockReturnValue(of(MOCK_BOOKINGS[0]));
			
			store.create(payload);
			
			expect(apiSpy.createBooking).toHaveBeenCalledTimes(1);
			expect(apiSpy.createBooking).toHaveBeenCalledWith(payload);
		});
		
		it('should keep `isLoading` true while the request is pending', () => {
			apiSpy.createBooking.mockReturnValue(new Subject());
			
			store.create(payload);
			
			expect(store.isLoading()).toBe(true);
		});
		
		it('should clear `isLoading` after a successful create', () => {
			apiSpy.createBooking.mockReturnValue(of(MOCK_BOOKINGS[0]));
			
			store.create(payload);
			
			expect(store.isLoading()).toBe(false);
		});
		
		it('should record the error and clear `isLoading` on a 409 conflict', () => {
			const conflict = new HttpErrorResponse({ status: 409, statusText: 'Conflict' });
			apiSpy.createBooking.mockReturnValue(throwError(() => conflict));
			
			store.create(payload);
			
			expect(store.conflictError()).toBe(conflict as unknown as string);
			expect(store.isLoading()).toBe(false);
		});
	});
});
