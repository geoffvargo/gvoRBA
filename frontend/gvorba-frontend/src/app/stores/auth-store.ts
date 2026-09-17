import { computed, inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { User } from '../models/user.model';
import { LoginRequest } from '../models/login-request.model';
import { SignupRequest } from '../models/signup-request.model';
import { TokenStorageService } from '../services/token-storage-service';
import { finalize, map, Observable, shareReplay, switchMap, tap } from 'rxjs';

@Injectable({
	providedIn: 'root',
})
export class AuthStore {
	private apiService = inject(ApiService);
	private tokenStorage = inject(TokenStorageService);
	private inFlight: Observable<string> | null = null;
	
	private _user = signal<User | null>(null);
	private _authToken = signal('');
	private _isLoading = signal(false);
	private _error = signal<Error | null>(null);
	
	readonly user = this._user.asReadonly();
	readonly authToken = this._authToken.asReadonly();
	readonly isLoading = this._isLoading.asReadonly();
	readonly error = this._error.asReadonly();
	
	// `.role` can itself be null/undefined even when `_user` isn't, so both hops
	// need `?.` — otherwise a user payload with a missing role throws here
	// instead of falling back to ROLE_GUEST.
	role = computed(() => this._user()?.role?.roleName ?? 'ROLE_GUEST');
	isAuthenticated = computed(() => this.authToken() !== '');
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
					// Read the CURRENT token from storage rather than reusing the
					// `token` captured above: if this request 401'd and got
					// silently retried with a refreshed token (see authInterceptor),
					// `token` is now stale. Writing it back here would clobber the
					// valid token refresh() already stored.
					this._authToken.set(this.tokenStorage.getToken() ?? token);
				},
				error: err => {
					console.log(err);
					this.resetState();
				},
			});
		}
	}
	
	signUp(payload: SignupRequest) {
		this._isLoading.set(true);
		this.apiService.signup(payload).subscribe({
			next: (data: string) => {
				console.log(data);
				this._isLoading.set(false);
			},
			error: err => {
				console.log(err);
				this._isLoading.set(false);
			},
		});
	}
	
	refresh() {
		// Dedupe concurrent refresh calls (e.g. several guards/interceptors
		// firing around the same time): everyone gets the same in-flight
		// observable instead of hitting /refresh multiple times.
		if (this.inFlight) {
			return this.inFlight;
		}

		this.inFlight = this.apiService.refreshToken().pipe(
			tap(
				r => {
					this._authToken.set(r.jwtToken);
					this.tokenStorage.saveToken(r.jwtToken);
				}),
			// Re-fetch and set `_user` here too, not just the token. Previously
			// refresh() only updated `_authToken`, so `isAdmin`/`role` (which are
			// derived from `_user`) went stale or fell back to ROLE_GUEST any
			// time a silent refresh fired — isAuthenticated looked fine while
			// admin-only UI silently disappeared.
			switchMap(
				r => this.apiService.getCurrentUser().pipe(
					tap(user => this._user.set(user)),
					map(() => r.jwtToken),
				)),
			finalize(
				() => this.inFlight = null),
			shareReplay({
				bufferSize: 1,
				refCount: false,
			}),
		);

		return this.inFlight;
	}
	
	login(user: LoginRequest) {
		this._isLoading.set(true);
		return this.apiService.loginUser(user).pipe(
			tap(resp => {
				this.tokenStorage.saveToken(resp.jwtToken);
				this._authToken.set(resp.jwtToken);
			}),
			switchMap(() => this.apiService.getCurrentUser()),
			tap({
				next: currUser => {
					this._user.set(currUser);
					this._isLoading.set(false);
				},
				error: err => {
					console.error(err);
					this.resetState();
					this._isLoading.set(false);
				},
			}),
		);
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
		this._user.set(null);
		this._authToken.set('');
	}
}
