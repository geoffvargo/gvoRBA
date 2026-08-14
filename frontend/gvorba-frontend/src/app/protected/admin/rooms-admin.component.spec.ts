import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { RoomsAdminComponent } from './rooms-admin.component';

describe('RoomsAdminComponent', () => {
  let component: RoomsAdminComponent;
  let fixture: ComponentFixture<RoomsAdminComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoomsAdminComponent],
      providers: [provideRouter([])],
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
