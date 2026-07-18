import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { RoomDetailsComponent } from './room-details.component';

describe('RoomDetailsComponent', () => {
	let component: RoomDetailsComponent;
	let fixture: ComponentFixture<RoomDetailsComponent>;
	
	beforeEach(async () => {
		await TestBed.configureTestingModule({
				imports: [RoomDetailsComponent],
				providers: [
					provideRouter([]),
					provideHttpClient(),
					provideHttpClientTesting(),
				],
			})
			.compileComponents();
		
		fixture = TestBed.createComponent(RoomDetailsComponent);
		component = fixture.componentInstance;
		await fixture.whenStable();
	});
	
	describe('dateTimeMapper', () => {
		it('returns 25 from Date object', () => {
			const date = new Date('2026-07-13T13:30:00');
			expect(component.timeMapper(date)).toBe(25);
		});
		
		it('returns something', () => {
			const date = new Date('2026-07-13T13:38:00');
			expect(component.timeMapper(date)).toBe(26);
		});
	});
	
	it('should create', () => {
		expect(component).toBeTruthy();
	});
	
	describe('dayMapper', () => {
		it('actually runs', () => {
			const date = new Date('2026-07-13T13:38:00');
			const ans = component.dayMapper(date);
			console.log("ans: {}", ans);
		});
	});
});
