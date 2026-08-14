import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { UsersAdminComponent } from './users-admin.component';

describe('UsersAdminComponent', () => {
  let component: UsersAdminComponent;
  let fixture: ComponentFixture<UsersAdminComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsersAdminComponent],
      providers: [provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(UsersAdminComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
