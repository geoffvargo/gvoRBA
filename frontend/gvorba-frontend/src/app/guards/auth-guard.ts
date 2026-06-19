import { CanActivateFn, RedirectCommand, Router } from '@angular/router';
import { inject } from '@angular/core';
import { TokenStorageService } from '../services/token-storage-service';
import { JwtHelperService } from '@auth0/angular-jwt';

export const authGuard: CanActivateFn = (route, state) => {
	const router = inject(Router);
	const tokenService = inject(TokenStorageService);
	const jwtHelper = inject(JwtHelperService);
	const token = tokenService.getToken();
	
	if (token !== null && !jwtHelper.isTokenExpired(token)) {
		return true;
	}
	
	return new RedirectCommand(router.parseUrl('/access-denied'));
};
