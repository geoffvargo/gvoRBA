import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Ping } from '../ping.interface';
import { environment } from '../../environments/environment';
import { LoginRequest } from '../models/login-request.model';
import { LoginResponse } from '../models/login-response.model';
import { User } from '../models/user.model';
import { Room } from '../models/room.model';
import { BehaviorSubject } from 'rxjs';
import { TokenStorageService } from './token-storage-service';
import { Booking } from '../models/booking.model';
import { CreateRoomRequest } from '../models/create-room.request';
import { UpdateRoomRequest } from '../models/update-room.request';
import { BookingRequest } from '../models/booking-request.model';
import { Role } from '../models/role.model';

// const baseURL = '/api';

@Injectable({
	providedIn: 'root',
})
export class ApiService {
	private httpClient = inject(HttpClient);
	private baseUrl = environment.apiBaseUrl;
	private tokenService = inject(TokenStorageService);
	private isLoggedInSubject = new BehaviorSubject<boolean>(false);
	
	// loggedIn = new BehaviorSubject<boolean>(true);
	isloggedIn$ = this.isLoggedInSubject.asObservable();
	
	notifyLoggedIn() {
		this.isLoggedInSubject.next(true);
	}
	
	loginUser(data: LoginRequest) {
		return this.httpClient.post<LoginResponse>(`${this.baseUrl}/api/auth/public/signin`, data);
	}
	
	logout() {
		this.tokenService.removeToken();
		
		this.isLoggedInSubject.next(false);
	}
	
	getUser() {
		return this.httpClient.get<User>(`${this.baseUrl}/api/auth/getUser`);
	}
	
	getPing() {
		return this.httpClient.get<Ping>(`${this.baseUrl}/api/ping`);
	}
	
	getRooms() {
		return this.httpClient.get<Room[]>(`${this.baseUrl}/api/rooms`);
	}
	
	getRoom(id: number) {
		return this.httpClient.get<Room>(`${this.baseUrl}/api/rooms/${id}`);
	}
	
	getBookings() {
		return this.httpClient.get<Booking[]>(`${this.baseUrl}/api/bookings`);
	}
	
	getMyBookings() {
		return this.httpClient.get<Booking[]>(`${this.baseUrl}/api/bookings/me`);
	}
	
	getCurrentUser() {
		return this.httpClient.get<User>(`${this.baseUrl}/api/auth/me`);
	}
	
	getRoomBookings(id: string, date: Date) {
		return this.httpClient.get<Booking[]>(
			`${this.baseUrl}/api/rooms/${id}/bookings?date=${date.toISOString().slice(0, 19)}`);
	}
	
	createRoom(data: CreateRoomRequest) {
		return this.httpClient.post<Room>(`${this.baseUrl}/api/add-room`, data);
	}
	
	updateRoom(id: number, payload: UpdateRoomRequest) {
		return this.httpClient.put<Room>(`${this.baseUrl}/api/rooms/${id}`, payload);
	}
	
	deactivateRoom(id: number) {
		return this.httpClient.delete<Room>(`${this.baseUrl}/api/rooms/${id}`);
	}
	
	cancelBooking(id: number) {
		return this.httpClient.delete<Booking>(`${this.baseUrl}/api/bookings/delete/${id}`);
	}
	
	createBooking(payload: BookingRequest) {
		return this.httpClient.post<Booking>(`${this.baseUrl}/api/add-booking`, payload);
	}
	
	loadUsers() {
		return this.httpClient.get<User[]>(`${this.baseUrl}/api/users`);
	}
	
	updateRole(id: number, payload: Role) {
		return this.httpClient.patch<Role>(`${this.baseUrl}/api/users/${id}/roles`, payload);
	}
	
	toggleActive(id: number) {
		return this.httpClient.patch<Role>(`${this.baseUrl}/api/users/${id}/toggle-active`, '');
	}
}
