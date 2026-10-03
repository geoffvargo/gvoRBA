import { HttpInterceptorFn } from '@angular/common/http';
import { inject, Injector } from '@angular/core';
import { TokenStorageService } from '../services/token-storage-service';
import { catchError, switchMap } from 'rxjs';
import { AuthStore } from '../stores/auth-store';

// Attaches access token; on 401, refreshes and retries once.
export const authInterceptor: HttpInterceptorFn = (req, next) => {
	console.log('authInterceptor', req); // debug leftover

	const tokenService = inject(TokenStorageService);
	const token = tokenService.getToken();
	// Resolve AuthStore lazily (only on 401). AuthStore's constructor fires
	// getCurrentUser() through HttpClient, which runs this interceptor while
	// AuthStore is still being constructed — injecting it eagerly here is NG0200.
	const injector = inject(Injector);

	// withCredentials always set: refresh token is an HttpOnly cookie
	const authReq = token ?
	                req.clone({
		                withCredentials: true,
		                setHeaders: { Authorization: `Bearer ${token}` },
	                }) :
	                req.clone({ withCredentials: true });

	return next(authReq).pipe(
		catchError(err => {
			if (err.status !== 401) {
				throw err; // not an auth error, nothing to do here
			}

			if (req.url.includes('/api/auth/public/refresh')) {
				throw err; // refresh itself failed — avoid infinite loop
			}

			// dedupes concurrent refreshes internally
			return injector.get(AuthStore).refresh().pipe(
				switchMap(newToken => {
					// replay original request once with the new token
					const retried = req.clone({
						withCredentials: true,
						setHeaders: { Authorization: `Bearer ${newToken}` },
					});
					return next(retried);
				}),
			);
		}),
	);
};

