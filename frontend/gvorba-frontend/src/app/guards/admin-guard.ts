import { inject } from '@angular/core';
import { CanActivateFn, RedirectCommand, Router } from '@angular/router';
import { TokenStorageService } from '../services/token-storage-service';
import { JwtHelperService } from '@auth0/angular-jwt';
import { AuthStore } from '../stores/auth-store';
import { catchError, map, of } from 'rxjs';

export const adminGuard: CanActivateFn = () => {
	const router = inject(Router);
	const tokenService = inject(TokenStorageService);
	const jwtHelper = inject(JwtHelperService);
	const authStore = inject(AuthStore);
	
	const token = tokenService.getToken();

	// Fast path: stored token is still valid, decide synchronously.
	if (token !== null && !jwtHelper.isTokenExpired(token)) {
		return hasAdminRole(jwtHelper, token) ? true : redirect(router);
	}

	// Token missing/expired: try a silent refresh before giving up, then
	// re-check the role against whatever token comes back.
	return authStore.refresh().pipe(
		map(newToken => hasAdminRole(jwtHelper, newToken) ? true : redirect(router)),
		catchError(() => of(redirect(router))),
	);
};

function redirect(router: Router) {
	return new RedirectCommand(router.parseUrl('/access-denied'));
}

/** Use JwtHelperService's own decoder instead of a hand-rolled
		atob(token.split('.')[1]): JWT payloads are base64url, not base64, so raw
		atob() throws InvalidCharacterError whenever the payload happens to
		contain a "-" or "_" — decodeToken() handles that encoding correctly. */
const hasAdminRole = (jwtHelper: JwtHelperService, token: string): boolean => {
	return jwtHelper.decodeToken(token)?.['roles'] === 'ROLE_ADMIN';
};
