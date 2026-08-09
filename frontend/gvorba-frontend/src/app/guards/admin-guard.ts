import { inject } from '@angular/core';
import { CanActivateFn, RedirectCommand, Router } from '@angular/router';
import { TokenStorageService } from '../services/token-storage-service';
import { JwtHelperService } from '@auth0/angular-jwt';

export const adminGuard: CanActivateFn = () => {
	const router = inject(Router);
	const tokenService = inject(TokenStorageService);
	const jwtHelper = inject(JwtHelperService);
	
	const token = tokenService.getToken();
	
	if (token !== null && !jwtHelper.isTokenExpired(token)) {
		const roleStr = JSON.parse(atob(token.split('.')[1]))['roles'];
		if (roleStr === 'ROLE_ADMIN') {
			return true;
		}
	}
	
	return new RedirectCommand(router.parseUrl('/access-denied'));
};
