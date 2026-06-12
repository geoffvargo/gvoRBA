import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Ping } from './ping.interface';
import { environment } from '../environments/environment';
import { Observable } from 'rxjs';

// const baseURL = '/api';

@Injectable({
	providedIn: 'root',
})
export class ApiService {
	private http = inject(HttpClient);
	private baseUrl = environment.apiBaseUrl;
	
	getPing() {
		return this.http.get<Ping>(`${this.baseUrl}/api/ping`);
	}
}
