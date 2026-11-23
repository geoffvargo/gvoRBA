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
	
	getMfaPending() {
		return sessionStorage.getItem('mfa_pending') === 'true';
	}
	
	saveMfaPending(pending: boolean) {
		sessionStorage.setItem('mfa-pending', String(pending));
	}
	
	removeMfaPending() {
		sessionStorage.removeItem('mfa-pending');
	}
}
