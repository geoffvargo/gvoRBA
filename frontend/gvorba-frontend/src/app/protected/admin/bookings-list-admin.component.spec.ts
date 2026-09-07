import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { BookingsListAdminComponent } from './bookings-list-admin.component';

describe('BookingsAdminComponent', () => {
  let component: BookingsListAdminComponent;
  let fixture: ComponentFixture<BookingsListAdminComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BookingsListAdminComponent],
      providers: [provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(BookingsListAdminComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
