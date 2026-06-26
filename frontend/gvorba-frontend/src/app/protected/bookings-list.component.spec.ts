import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { BookingsListComponent } from './bookings-list.component';
import { ApiService } from '../services/api.service';

describe('BookingsListComponent', () => {
  let component: BookingsListComponent;
  let fixture: ComponentFixture<BookingsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BookingsListComponent],
      providers: [
        {
          provide: ApiService,
          useValue: {
            getBookings: () => of([])
          }
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BookingsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
