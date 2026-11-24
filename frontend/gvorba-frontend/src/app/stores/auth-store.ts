import { computed, inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { LoginRequest } from '../models/login-request.model';
import { SignupRequest } from '../models/signup-request.model';
import { TokenStorageService } from '../services/token-storage-service';
import { finalize, map, Observable, of, shareReplay, switchMap, tap } from 'rxjs';
import { HotToastService } from '@ngxpert/hot-toast';
import { UserStore } from './user-store';
import { OtpRequest } from '../models/two-factor.models';
import { LoginResponse } from '../models/login-response.model';

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
	private _mfaPending = signal(false);
	
	readonly authToken = this._authToken.asReadonly();
	readonly isLoading = this._isLoading.asReadonly();
	readonly error = this._error.asReadonly();
	readonly mfaPending = this._mfaPending.asReadonly();
	
	isAuthenticated = computed(() => this.authToken() !== '' && !this.mfaPending());
	
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
		
		this._authToken.set(token);
		this._mfaPending.set(this.tokenStorage.getMfaPending());
		
		if (this._mfaPending()) {
			return;    // a pending token can't call /api/auth/me; the /mfa page takes it from here
		}
		
		this.userStore.loadCurrentUser().subscribe({
			next: data => {
				console.log('currUser(): ', data);
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
	
	/* MARK: refresh() */
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
					this.applySession(r);
				}),
			// Re-fetch the user here too, not just the token. Previously
			// refresh() only updated the token, so `isAdmin`/`role` (derived
			// from the user in UserStore) went stale or fell back to ROLE_GUEST
			// any time a silent refresh fired — isAuthenticated looked fine
			// while admin-only UI silently disappeared.
			switchMap(
				r => r.mfaRequired
				     ? of(r.jwtToken)
				     : this.userStore.loadCurrentUser().pipe(
						map(
							() => r.jwtToken,
						),
					),
			),
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
			tap(resp => this.applySession(resp)),
			switchMap(resp => {
				// Decide what this step emits when resp.mfaRequired is true vs. false.
				if (resp.mfaRequired) {
					return of(resp);
				}
				return this.userStore.loadCurrentUser().pipe(map(() => resp));
			}),
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
		this.tokenStorage.removeMfaPending();
		this._mfaPending.set(false);
		this._authToken.set('');
		
	}
	
	verifyCode(code: OtpRequest) {
		return this.apiService.verifyTwoFactor(code).pipe(
			tap(data => this.applySession(data)),
			switchMap(data => this.userStore.loadCurrentUser().pipe(
				map(() => data),
			)),
		);
	}
	
	recoverWithCode(recoveryCode: string) {
		return this.apiService.recoverTwoFactor({ recoveryCode }).pipe(
			tap(data => this.applySession(data)),
			switchMap(data => this.userStore.loadCurrentUser().pipe(
				map(() => data),
			)),
		);
	}
	
	applySession(resp: LoginResponse) {
		this.tokenStorage.saveToken(resp.jwtToken);
		this.tokenStorage.saveMfaPending(resp.mfaRequired);
		
		this._authToken.set(resp.jwtToken);
		this._mfaPending.set(resp.mfaRequired);
	}
	
	markMfaRequired() {
		this.tokenStorage.saveMfaPending(true);
		this._mfaPending.set(true);
	}
}
