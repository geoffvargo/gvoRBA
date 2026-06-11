import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Ping } from './ping.interface';

const baseURL = '/api';

@Injectable({
	providedIn: 'root',
})
export class ApiService {
	private http = inject(HttpClient);
	
	getPing() {
		return this.http.get<Ping>(`${baseURL}/ping`);
	}
}
