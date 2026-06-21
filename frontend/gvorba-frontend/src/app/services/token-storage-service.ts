import { Injectable } from '@angular/core';

@Injectable({
	providedIn: 'root',
})
export class TokenStorageService {
	getToken() {
		return sessionStorage.getItem('auth-token');
	}
	
	saveToken(token: string) {
		return sessionStorage.setItem('auth-token', token);
	}
	
	removeToken() {
		return sessionStorage.removeItem('auth-token');
	}
}
