import { CanActivateFn, RedirectCommand, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthStore } from '../stores/auth-store';
import { TokenStorageService } from '../services/token-storage-service';

export const mfaPendingGuard: CanActivateFn = () => {
	const router = inject(Router);
	const authStore = inject(AuthStore);
	const tokenService = inject(TokenStorageService);
	
	if (authStore.mfaPending() && tokenService.getToken()) {
		return true;
	}
	
	return new RedirectCommand(router.parseUrl('/'));
};
