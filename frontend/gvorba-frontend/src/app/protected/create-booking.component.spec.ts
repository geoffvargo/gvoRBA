import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { CreateBookingComponent, nextWeekdayFrom, isSameLocalDay, formatLocalDate } from './create-booking.component';

describe('CreateBookingComponent', () => {
  let component: CreateBookingComponent;
  let fixture: ComponentFixture<CreateBookingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateBookingComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateBookingComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('nextWeekdayFor', () => {
  it('returns the following day when it is a weekday', () => {
    // Monday 2026-08-24 -> Tuesday
    const from = new Date(2026, 7, 24, 10, 30, 0);
    const result = nextWeekdayFrom(from);

    expect(result.getFullYear()).toBe(2026);
    expect(result.getMonth()).toBe(7);
    expect(result.getDate()).toBe(25);
    expect(result.getDay()).toBe(2); // Tuesday
  });

  it('skips the weekend when starting from a Friday', () => {
    // Friday 2026-08-28 -> Monday 2026-08-31
    const from = new Date(2026, 7, 28, 9, 0, 0);
    const result = nextWeekdayFrom(from);

    expect(result.getDate()).toBe(31);
    expect(result.getDay()).toBe(1); // Monday
  });

  it('skips Sunday when starting from a Saturday', () => {
    // Saturday 2026-08-29 -> Monday 2026-08-31
    const from = new Date(2026, 7, 29, 12, 0, 0);
    const result = nextWeekdayFrom(from);

    expect(result.getDate()).toBe(31);
    expect(result.getDay()).toBe(1); // Monday
  });

  it('returns Monday when starting from a Sunday', () => {
    // Sunday 2026-08-30 -> Monday 2026-08-31
    const from = new Date(2026, 7, 30, 0, 0, 0);
    const result = nextWeekdayFrom(from);

    expect(result.getDate()).toBe(31);
    expect(result.getDay()).toBe(1); // Monday
  });

  it('rolls over into the next month across a month boundary', () => {
    // Friday 2026-01-30 -> Monday 2026-02-02
    const from = new Date(2026, 0, 30);
    const result = nextWeekdayFrom(from);

    expect(result.getFullYear()).toBe(2026);
    expect(result.getMonth()).toBe(1); // February
    expect(result.getDate()).toBe(2);
    expect(result.getDay()).toBe(1); // Monday
  });

  it('rolls over into the next year across a year boundary', () => {
    // Wednesday 2025-12-31 -> Thursday 2026-01-01
    const from = new Date(2025, 11, 31);
    const result = nextWeekdayFrom(from);

    expect(result.getFullYear()).toBe(2026);
    expect(result.getMonth()).toBe(0); // January
    expect(result.getDate()).toBe(1);
    expect(result.getDay()).toBe(4); // Thursday
  });

  it('preserves the time-of-day components from the input', () => {
    const from = new Date(2026, 7, 24, 14, 45, 30, 500);
    const result = nextWeekdayFrom(from);

    expect(result.getHours()).toBe(14);
    expect(result.getMinutes()).toBe(45);
    expect(result.getSeconds()).toBe(30);
    expect(result.getMilliseconds()).toBe(500);
  });

  it('does not mutate the input date', () => {
    const from = new Date(2026, 7, 24, 10, 30, 0);
    const originalTime = from.getTime();

    nextWeekdayFrom(from);

    expect(from.getTime()).toBe(originalTime);
  });
});

describe('isSameLocalDay', () => {
  it('returns true for two Date objects on the same calendar day, different times', () => {
    const a = new Date(2026, 7, 30, 0, 0, 0);
    const b = new Date(2026, 7, 30, 23, 59, 59);

    expect(isSameLocalDay(a, b)).toBe(true);
  });

  it('returns true when compared with itself', () => {
    const a = new Date(2026, 7, 30, 12, 0, 0);

    expect(isSameLocalDay(a, a)).toBe(true);
  });

  it('returns false for a different day within the same month', () => {
    const a = new Date(2026, 7, 30);
    const b = new Date(2026, 7, 29);

    expect(isSameLocalDay(a, b)).toBe(false);
  });

  it('returns false for the same day-of-month in a different month', () => {
    const a = new Date(2026, 7, 30);
    const b = new Date(2026, 8, 30);

    expect(isSameLocalDay(a, b)).toBe(false);
  });

  it('returns false for the same month/day in a different year', () => {
    // Both fall on a Sunday, so a same-weekday check alone would wrongly match these.
    const a = new Date(2020, 7, 30);
    const b = new Date(2026, 7, 30);

    expect(isSameLocalDay(a, b)).toBe(false);
  });

  it('returns false for two dates in the same month that share a weekday but not a day-of-month', () => {
    // Aug 3 2026 and Aug 10 2026 are both Mondays.
    const a = new Date(2026, 7, 3);
    const b = new Date(2026, 7, 10);

    expect(isSameLocalDay(a, b)).toBe(false);
  });

  it('returns false across a year boundary even when month and day-of-month match', () => {
    const a = new Date(2025, 11, 31);
    const b = new Date(2026, 11, 31);

    expect(isSameLocalDay(a, b)).toBe(false);
  });

  it('is symmetric', () => {
    const a = new Date(2026, 7, 30, 8, 0, 0);
    const b = new Date(2026, 7, 30, 20, 0, 0);

    expect(isSameLocalDay(a, b)).toBe(isSameLocalDay(b, a));
  });
});

describe('formatLocalDate', () => {
  it('formats a regular date as YYYY-M-D', () => {
    // Sunday Aug 30 2026
    const date = new Date(2026, 7, 30);

    expect(formatLocalDate(date)).toBe('2026-8-30');
  });

  it('formats a single-digit month and day without zero-padding', () => {
    // Monday Jan 5 2026
    const date = new Date(2026, 0, 5);

    expect(formatLocalDate(date)).toBe('2026-1-5');
  });

  it('formats December correctly (2-digit month)', () => {
    // Wednesday Dec 31 2025
    const date = new Date(2025, 11, 31);

    expect(formatLocalDate(date)).toBe('2025-12-31');
  });

  it('formats the first day of a month', () => {
    // Saturday Aug 1 2026
    const date = new Date(2026, 7, 1);

    expect(formatLocalDate(date)).toBe('2026-8-1');
  });

  it('is not affected by time-of-day', () => {
    const morning = new Date(2026, 7, 30, 0, 0, 0);
    const night = new Date(2026, 7, 30, 23, 59, 59);

    expect(formatLocalDate(morning)).toBe(formatLocalDate(night));
  });
});
