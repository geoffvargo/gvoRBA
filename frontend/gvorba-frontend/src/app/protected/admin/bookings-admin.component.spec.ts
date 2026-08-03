import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BookingsAdminComponent } from './bookings-admin.component';

describe('BookingsAdminComponent', () => {
  let component: BookingsAdminComponent;
  let fixture: ComponentFixture<BookingsAdminComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BookingsAdminComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BookingsAdminComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
