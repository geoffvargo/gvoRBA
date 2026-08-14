import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AdminAddUserComponent } from './admin-add-user.component';

describe('AdminAddUserComponent', () => {
  let component: AdminAddUserComponent;
  let fixture: ComponentFixture<AdminAddUserComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminAddUserComponent],
      providers: [provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminAddUserComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
