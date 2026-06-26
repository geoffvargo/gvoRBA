import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { JWT_OPTIONS, JwtHelperService } from '@auth0/angular-jwt';
import { App } from './app';

describe('App', () => {
	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [App],
			providers: [
				provideRouter([]),
				provideHttpClient(),
				provideHttpClientTesting(),
				{ provide: JWT_OPTIONS, useValue: JWT_OPTIONS },
				JwtHelperService,
			],
		}).compileComponents();
	});
	
	it('should create the app', () => {
		const fixture = TestBed.createComponent(App);
		const app = fixture.componentInstance;
		expect(app).toBeTruthy();
	});
});
