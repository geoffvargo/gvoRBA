import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { Oauth2CallbackComponent } from './oauth2-callback.component';
import { AuthStore } from '../stores/auth-store';

describe('Oauth2CallbackComponent', () => {
  let component: Oauth2CallbackComponent;
  let fixture: ComponentFixture<Oauth2CallbackComponent>;
  let navigate: ReturnType<typeof vi.fn>;
  let refresh: ReturnType<typeof vi.fn>;
  let route: { snapshot: { queryParamMap: ReturnType<typeof convertToParamMap> } };

  beforeEach(async () => {
    navigate = vi.fn().mockResolvedValue(true);
    refresh = vi.fn(() => of('token'));
    route = { snapshot: { queryParamMap: convertToParamMap({}) } };

    await TestBed.configureTestingModule({
      imports: [Oauth2CallbackComponent],
      providers: [
        { provide: Router, useValue: { navigate } },
        { provide: ActivatedRoute, useValue: route },
        { provide: AuthStore, useValue: { refresh } },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(Oauth2CallbackComponent);
    component = fixture.componentInstance;
  });

  it('should create and navigate home on refresh success', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component).toBeTruthy();
    expect(refresh).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['home']);
  });

  it('should navigate to root on refresh error', async () => {
    refresh.mockReturnValue(throwError(() => new Error('refresh failed')));

    fixture.detectChanges();
    await fixture.whenStable();

    expect(navigate).toHaveBeenCalledWith(['']);
  });

  it('should navigate with oauth error query param when callback has error', async () => {
    route.snapshot.queryParamMap = convertToParamMap({ error: 'access_denied' });
    refresh.mockReturnValue(throwError(() => new Error('refresh failed')));

    fixture.detectChanges();
    await fixture.whenStable();

    expect(navigate).toHaveBeenCalledWith([''], {
      queryParams: { error: 'access_denied' },
    });
  });
});
