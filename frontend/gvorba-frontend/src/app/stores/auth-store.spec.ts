import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, type Mock, vi } from 'vitest';
import { of } from 'rxjs';

import { AuthStore } from './auth-store';
import { ApiService } from '../services/api.service';
import { TokenStorageService } from '../services/token-storage-service';
import { User } from '../models/user.model';
import { Role } from '../models/role.model';
import { LoginRequest } from '../models/login-request.model';

describe('AuthStore', () => {
	let service: AuthStore;
	let apiSpy: {
		getCurrentUser: Mock,
		signup: Mock,
		loginUser: Mock,
		logout: Mock,
	};
	let tokenStorageSpy: {
		saveToken: Mock,
	};
	
	beforeEach(() => {
		apiSpy = {
			getCurrentUser: vi.fn(),
			signup: vi.fn(),
			loginUser: vi.fn(),
			logout: vi.fn(),
		};
		tokenStorageSpy = {
			saveToken: vi.fn(),
		};
		
		sessionStorage.removeItem('auth-token');
		
		TestBed.configureTestingModule({
			providers: [
				AuthStore,
				{ provide: ApiService, useValue: apiSpy },
				{ provide: TokenStorageService, useValue: tokenStorageSpy },
			],
		});
		
		service = TestBed.inject(AuthStore);
	});
	
	it('should be created', () => {
		expect(service).toBeTruthy();
	});
	
	describe('user', () => {
		it('should start out as null when there is no stored auth token', () => {
			expect(service.user()).toBeNull();
		});
		
		it('should be read-only, exposing no `.set`/`.update` methods', () => {
			expect((service.user as unknown as { set?: unknown }).set).toBeUndefined();
			expect((service.user as unknown as { update?: unknown }).update).toBeUndefined();
		});
		
		it('should reflect the current user after a successful login', () => {
			const user = new User(1, 'jane@example.com', 'Jane', new Role(1, 'ROLE_MEMBER'));
			apiSpy.loginUser.mockReturnValue(of({ jwtToken: 'token-123' }));
			apiSpy.getCurrentUser.mockReturnValue(of(user));
			
			service.login({} as LoginRequest).subscribe();
			
			expect(service.user()).toEqual(user);
			console.log('user: ', user);
			expect(service.user()?.id).toEqual(1);
		});
		
		it('should reset to null after logout', () => {
			const user = new User(1, 'jane@example.com', 'Jane', new Role(1, 'ROLE_MEMBER'));
			apiSpy.loginUser.mockReturnValue(of({ jwtToken: 'token-123' }));
			apiSpy.getCurrentUser.mockReturnValue(of(user));
			service.login({} as LoginRequest).subscribe();
			
			service.logout();
			
			expect(service.user()).toBeNull();
		});
	});
});
