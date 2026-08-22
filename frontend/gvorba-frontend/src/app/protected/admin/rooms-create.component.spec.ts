import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { RoomsCreateComponent } from './rooms-create.component';

describe('RoomsCreateComponent', () => {
  let component: RoomsCreateComponent;
  let fixture: ComponentFixture<RoomsCreateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoomsCreateComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RoomsCreateComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
