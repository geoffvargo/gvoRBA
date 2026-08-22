import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RoomManageComponent } from './room-manage.component';

describe('RoomManageComponent', () => {
  let component: RoomManageComponent;
  let fixture: ComponentFixture<RoomManageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoomManageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RoomManageComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
