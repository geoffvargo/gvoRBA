import { TestBed } from '@angular/core/testing';

import { RoomStore } from './room-store';

describe('RoomStore', () => {
  let service: RoomStore;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RoomStore);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
