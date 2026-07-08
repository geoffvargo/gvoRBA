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
	
	getCurrentUser() {
		return this.httpClient.get<User>(`${this.baseUrl}/api/auth/me`);
	}
	
	getRoomBookings(id: string, date: string) {
		return this.httpClient.get<Booking[]>(`${this.baseUrl}/api/rooms/${id}/bookings/${date}`);
	}
	
	createRoom(data: CreateRoomRequest) {
		return this.httpClient.post<Room>(`${this.baseUrl}/api/add-room`, data);
	}
}
