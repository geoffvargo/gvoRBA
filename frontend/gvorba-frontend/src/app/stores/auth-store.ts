import { computed, inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { LoginRequest } from '../models/login-request.model';
import { SignupRequest } from '../models/signup-request.model';
import { TokenStorageService } from '../services/token-storage-service';
import { finalize, map, Observable, shareReplay, switchMap, tap } from 'rxjs';
import { HotToastService } from '@ngxpert/hot-toast';
import { UserStore } from './user-store';

@Injectable({
	providedIn: 'root',
})
export class AuthStore {
	private apiService = inject(ApiService);
	private tokenStorage = inject(TokenStorageService);
	/// One-way dependency: AuthStore → UserStore. UserStore must never inject
	/// AuthStore, or Angular throws a circular-DI error (NG0200).
	private userStore = inject(UserStore);
	private inFlight: Observable<string> | null = null;
	private toast = inject(HotToastService);
	
	private _authToken = signal('');
	private _isLoading = signal(false);
	private _error = signal<Error | null>(null);
	
	readonly authToken = this._authToken.asReadonly();
	readonly isLoading = this._isLoading.asReadonly();
	readonly error = this._error.asReadonly();
	
	isAuthenticated = computed(() => this.authToken() !== '');
	
	constructor() {
		this.restoreSession();
	}
	
	/* MUTATORS */
	
	/** On app start, if a token survived in sessionStorage, re-fetch the user and re-arm the token signal. */
	private restoreSession() {
		const token = sessionStorage.getItem('auth-token');
		if (!token) {
			return;
		}
		
		this.userStore.loadCurrentUser().subscribe({
			next: data => {
				console.log(data);
			},
			error: error => {
				console.log(error);
				this.toast.error(error);
			},
		});
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
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
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
			// Re-fetch the user here too, not just the token. Previously
			// refresh() only updated the token, so `isAdmin`/`role` (derived
			// from the user in UserStore) went stale or fell back to ROLE_GUEST
			// any time a silent refresh fired — isAuthenticated looked fine
			// while admin-only UI silently disappeared.
			switchMap(
				r => this.userStore.loadCurrentUser().pipe(
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
			switchMap(() => this.userStore.loadCurrentUser()),
			tap({
				next: () => {
					this._isLoading.set(false);
				},
				error: err => {
					console.error(err);
					this.resetState();
					this.toast.error(err.message, {
						position: 'bottom-center',
						dismissible: true,
					});
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
		this.userStore.clearCurrentUser();
		this._authToken.set('');
	}
}
