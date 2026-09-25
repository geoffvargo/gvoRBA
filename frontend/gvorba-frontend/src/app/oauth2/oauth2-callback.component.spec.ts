import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { Oauth2CallbackComponent } from './oauth2-callback.component';
import { AuthStore } from '../stores/auth-store';

describe('Oauth2CallbackComponent', () => {
  let component: Oauth2CallbackComponent;
  let fixture: ComponentFixture<Oauth2CallbackComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Oauth2CallbackComponent],
      providers: [
        { provide: Router, useValue: { navigate: vi.fn().mockResolvedValue(true) } },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap({}) },
          },
        },
        { provide: AuthStore, useValue: { refresh: vi.fn(() => of('token')) } },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(Oauth2CallbackComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
