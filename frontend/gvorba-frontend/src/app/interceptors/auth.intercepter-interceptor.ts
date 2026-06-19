import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenStorageService } from '../services/token-storage-service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
	console.log('authInterceptor', req);
	
	const tokenService = inject(TokenStorageService);
	const token = tokenService.getToken();
	
	const headers: Record<string, string | string[]> = {
		'Authorization': token ? `Bearer ${token}` : '',
		'Content-Type': 'application/json',
		'Accept': 'application/json',
	};
	
	const authReq = req.clone({
		setHeaders: headers,
	});
	
	return next(authReq);
};
