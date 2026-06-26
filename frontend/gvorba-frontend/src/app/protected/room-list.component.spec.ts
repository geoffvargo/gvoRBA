import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { RoomListComponent } from './room-list.component';
import { ApiService } from '../services/api.service';

describe('RoomListComponent', () => {
  let component: RoomListComponent;
  let fixture: ComponentFixture<RoomListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoomListComponent],
      providers: [
        {
          provide: ApiService,
          useValue: {
            getRooms: () => of([])
          }
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RoomListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
