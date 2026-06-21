import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Ping } from '../ping.interface';
import { environment } from '../../environments/environment';
import { LoginRequest } from '../models/login-request.model';
import { LoginResponse } from '../models/login-response.model';
import { User } from '../models/user.model';
import { Room } from '../models/room.model';

// const baseURL = '/api';

@Injectable({
	providedIn: 'root',
})
export class ApiService {
	private httpClient = inject(HttpClient);
	private baseUrl = environment.apiBaseUrl;
	
	loginUser(data: LoginRequest) {
		return this.httpClient.post<LoginResponse>(`${this.baseUrl}/api/auth/public/signin`, data);
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
}
