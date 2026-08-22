import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { RoomsAdminComponent } from './rooms-admin.component';
import { ApiService } from '../../services/api.service';

describe('RoomsAdminComponent', () => {
  let component: RoomsAdminComponent;
  let fixture: ComponentFixture<RoomsAdminComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoomsAdminComponent],
      providers: [
        provideRouter([]),
        {
          provide: ApiService,
          useValue: {
            getRooms: () => of([]),
          },
        },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(RoomsAdminComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
