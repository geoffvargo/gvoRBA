import { CanActivateFn, RedirectCommand, Router } from '@angular/router';
import { inject } from '@angular/core';
import { TokenStorageService } from '../services/token-storage-service';
import { JwtHelperService } from '@auth0/angular-jwt';
import { AuthStore } from '../stores/auth-store';
import { catchError, map, of } from 'rxjs';

export const authGuard: CanActivateFn = (_route, _state) => {
	const router = inject(Router);
	const tokenService = inject(TokenStorageService);
	const jwtHelper = inject(JwtHelperService);
	const authStore = inject(AuthStore);
	const token = tokenService.getToken();
	
	if (token !== null && !jwtHelper.isTokenExpired(token)) {
		return true;
	}
	
	return authStore.refresh().pipe(
		map(() => true),
		catchError(() =>
			of(new RedirectCommand(router.parseUrl('/access-denied')))),
	);
};
