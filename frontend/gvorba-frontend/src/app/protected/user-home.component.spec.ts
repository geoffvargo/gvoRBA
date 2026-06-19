import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { JWT_OPTIONS, JwtHelperService } from '@auth0/angular-jwt';

import { UserHomeComponent } from './user-home.component';

describe('UserLandingComponent', () => {
  let component: UserHomeComponent;
  let fixture: ComponentFixture<UserHomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserHomeComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: JWT_OPTIONS, useValue: JWT_OPTIONS },
        JwtHelperService,
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserHomeComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
