// frontend/gvorba-frontend/src/app/guards/admin-guard-guard.spec.ts
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, RedirectCommand, RouterStateSnapshot } from '@angular/router';
import { JwtHelperService } from '@auth0/angular-jwt';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { adminGuard } from './admin-guard';
import { TokenStorageService } from '../services/token-storage-service';

// Paste real tokens obtained from your backend:
const ADMIN_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTc4NTcyNjMyOH0.IgEPVIUGVo6QiqUxnYcMQ6baV9GFOsBn-SUhGjRMNow';
const USER_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwicm9sZXMiOiJST0xFX1VTRVIiLCJpYXQiOjE3ODU3MjYzMjh9.CanXF3Hqg8vKqXjBOkQ2g0OlZpLgvDfS6Pqi0_mtg1I';
const EXPIRED_ADMIN_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwicm9sZXMiOiJST0xFX1VTRVIiLCJpYXQiOjE3ODU3MjYzMjgsImV4cCI6IjEwMDAwMDAwMDAifQ.e6Aq00P9x6YxWaR79FMp9k8jsRSiCuKMQDayzsbrThI';
const NO_ROLES_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNzg1NzI2MzI4fQ.gL3VkZYCCKloGgognlJRbcTiHxp2ikPWqjxIRNOLz5k';

describe('adminGuardGuard', () => {
	const route = {} as ActivatedRouteSnapshot;
	const state = {} as RouterStateSnapshot;
	
	// The guard uses inject(); this wrapper supplies the required context.
	const runGuard = () =>
		TestBed.runInInjectionContext(() =>
			adminGuard(route, state),
		);
	
	let tokenService: { getToken: ReturnType<typeof vi.fn> };
	let jwtHelper: JwtHelperService;
	
	beforeEach(() => {
		tokenService = { getToken: vi.fn() };
		jwtHelper = new JwtHelperService(); // real decode + real expiry check
		
		TestBed.configureTestingModule({
			providers: [
				provideRouter([]),
				{ provide: TokenStorageService, useValue: tokenService },
				{ provide: JwtHelperService, useValue: jwtHelper },
			],
		});
	});
	
	it('grants access when a valid token carries ROLE_ADMIN', () => {
		tokenService.getToken.mockReturnValue(ADMIN_TOKEN);
		expect(runGuard()).toBe(true);
	});
	
	it('denies access when a valid token carries only ROLE_USER', () => {
		tokenService.getToken.mockReturnValue(USER_TOKEN);
		expect(runGuard()).toBeInstanceOf(RedirectCommand);
	});
	
	it('denies access when the token is expired', () => {
		tokenService.getToken.mockReturnValue(EXPIRED_ADMIN_TOKEN);
		expect(runGuard()).toBeInstanceOf(RedirectCommand);
	});
	
	it('denies access when no token is stored', () => {
		const isExpired = vi.spyOn(jwtHelper, 'isTokenExpired');
		tokenService.getToken.mockReturnValue(null);
		expect(runGuard()).toBeInstanceOf(RedirectCommand);
		expect(isExpired).not.toHaveBeenCalled(); // existence checked first
	});
	
	it('denies access when the token omits the roles claim', () => {
		tokenService.getToken.mockReturnValue(NO_ROLES_TOKEN);
		expect(runGuard()).toBeInstanceOf(RedirectCommand);
	});
	
	// it('denies access without crashing on a malformed token', () => {
	// 	tokenService.getToken.mockReturnValue('not-a-jwt');
	// 	expect(runGuard()).toBe(false);
	// });
});
