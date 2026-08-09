import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding, withRouterConfig } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { JWT_OPTIONS, JwtHelperService } from '@auth0/angular-jwt';
import { authInterceptor } from './interceptors/auth.intercepter-interceptor';
import { MAT_SELECT_CONFIG } from '@angular/material/select';

export const appConfig: ApplicationConfig = {
	providers: [
		provideBrowserGlobalErrorListeners(),
		provideRouter(routes,
			withComponentInputBinding(),
			withRouterConfig({
				onSameUrlNavigation: 'reload',
			})
		),
		provideHttpClient(),
		provideHttpClient(withInterceptors([authInterceptor])),
		{ provide: JWT_OPTIONS, useValue: JWT_OPTIONS },
		JwtHelperService,
		{ provide: MAT_SELECT_CONFIG, useValue: { panelClass: 'ds-select-panel' } },
	],
};
