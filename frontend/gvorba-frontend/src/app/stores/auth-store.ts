import { computed, inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { User } from '../models/user.model';
import { LoginRequest } from '../models/login-request.model';
import { SignupRequest } from '../models/signup-request.model';

@Injectable({
	providedIn: 'root',
})
export class AuthStore {
	private apiService = inject(ApiService);
	
	private _user = signal<User | null>(null);
	private _authToken = signal('');
	private _isLoading = signal(false);
	private _error = signal<Error | null>(null);
	
	readonly user = this._user.asReadonly();
	readonly authToken = this._authToken.asReadonly();
	readonly isLoading = this._isLoading.asReadonly();
	readonly error = this._error.asReadonly();
	
	role = computed(() => this._user()?.role.roleName ?? 'ROLE_GUEST');
	isAuthenticated = computed(() => this.authToken() !== null);
	isAdmin = computed(() => this.role() === 'ROLE_ADMIN');
	isMember = computed(() => this.role() === 'ROLE_MEMBER');
	
	constructor() {
		this.loadCurrentUser();
	}
	
	/* MUTATORS */
	loadCurrentUser() {
		const token = sessionStorage.getItem('auth-token');
		
		if (token) {
			this.apiService.getCurrentUser().subscribe({
				next: (data: User) => {
					this._user.set(data);
					console.log(this._user());
					this._authToken.set(token);
				},
				error: err => {
					console.log(err);
					this.resetState();
				},
			});
		}
	}
	
	register(payload: SignupRequest) {
		console.log(payload);
	}
	
	refresh() {
		console.log('refresh()');
	}
	
	login(user: LoginRequest) {
		this._isLoading.set(true);
		this.apiService.loginUser(user).subscribe({
			next: data => {
				this._authToken.set(data.jwtToken);
				console.log(data);
				this._isLoading.set(false);
			},
			error: err => {
				console.log(err);
				this._isLoading.set(false);
			},
		});
	}
	
	logout() {
		this.apiService.logout();
		this.resetState();
	}
	
	clearError() {
		this._error.set(null);
	}
	
	private resetState() {
		sessionStorage.removeItem('auth-token');
		this._user.set(new User());
		this._authToken.set('');
	}
}
