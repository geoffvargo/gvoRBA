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

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
