import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { LandingPage } from './landing-page';

describe('LandingPage', () => {
	let component: LandingPage;
	let fixture: ComponentFixture<LandingPage>;
	
	beforeEach(async () => {
		await TestBed.configureTestingModule({
				imports: [LandingPage],
				providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
			})
			.compileComponents();
		
		fixture = TestBed.createComponent(LandingPage);
		component = fixture.componentInstance;
		await fixture.whenStable();
	});
	
	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
