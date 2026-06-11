import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Ping } from './ping.interface';

const baseURL = 'http://localhost:8080/api';

@Injectable({
	providedIn: 'root',
})
export class ApiService {
	private http = inject(HttpClient);
	
	getPing() {
		return this.http.get<Ping>(`${baseURL}/ping`);
	}
}
