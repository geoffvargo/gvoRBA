import { Booking } from '../models/booking.model';

// Current user in most tests is user 100. Rooms 1-3 are the workhorses.
// Dates chosen relative to July 18, 2026 (SPEC's "current date"):
//   - past: June and early July 2026
//   - today: July 18, 2026
//   - future: late July / August 2026

export const MOCK_BOOKING_1 = new Booking(
	1, 1, 100,
	new Date('2026-06-15T09:00:00'),
	new Date('2026-06-15T10:00:00'),
	null, 'Past team standup', 'CONFIRMED',
);

export const MOCK_BOOKING_2 = new Booking(
	2, 2, 100,
	new Date('2026-07-01T13:00:00'),
	new Date('2026-07-01T14:30:00'),
	null, 'Client call — Acme', 'CONFIRMED',
);

// Cancelled past booking — exercises soft-delete display logic (FR-3.7)
export const MOCK_BOOKING_3 = new Booking(
	3, 1, 100,
	new Date('2026-07-05T10:00:00'),
	new Date('2026-07-05T11:00:00'),
	new Date('2026-07-04T18:22:00'),
	'Cancelled — rescheduled', 'CANCELLED',
);

// Today, mid-morning — most likely to appear in "today's day view" tests
export const MOCK_BOOKING_4 = new Booking(
	4, 1, 100,
	new Date('2026-07-18T10:00:00'),
	new Date('2026-07-18T11:00:00'),
	null, 'Sprint planning', 'CONFIRMED',
);

// Today, afternoon, different room
export const MOCK_BOOKING_5 = new Booking(
	5, 3, 100,
	new Date('2026-07-18T14:00:00'),
	new Date('2026-07-18T15:00:00'),
	null, '1:1 with manager', 'CONFIRMED',
);

// Minimum-length booking (15 min) — edge case per FR-3.1
export const MOCK_BOOKING_6 = new Booking(
	6, 2, 100,
	new Date('2026-07-20T09:00:00'),
	new Date('2026-07-20T09:15:00'),
	null, 'Quick sync', 'CONFIRMED',
);

// Maximum-length booking (4 hr) — edge case per FR-3.1
export const MOCK_BOOKING_7 = new Booking(
	7, 3, 100,
	new Date('2026-07-22T13:00:00'),
	new Date('2026-07-22T17:00:00'),
	null, 'Design workshop', 'CONFIRMED',
);

// Future booking by a DIFFERENT user — useful for room day-view tests
// where you want bookings not owned by the current user
export const MOCK_BOOKING_8 = new Booking(
	8, 1, 200,
	new Date('2026-07-25T10:00:00'),
	new Date('2026-07-25T11:30:00'),
	null, 'Interview panel', 'CONFIRMED',
);

// Another different-user booking, same room, same day as #8 — day-view sorting
export const MOCK_BOOKING_9 = new Booking(
	9, 1, 300,
	new Date('2026-07-25T14:00:00'),
	new Date('2026-07-25T15:00:00'),
	null, 'Vendor meeting', 'CONFIRMED',
);

// Far-future booking near the 30-day boundary (FR-3.1)
export const MOCK_BOOKING_10 = new Booking(
	10, 2, 100,
	new Date('2026-08-15T09:00:00'),
	new Date('2026-08-15T10:00:00'),
	null, 'Quarterly review', 'CONFIRMED',
);

// -----------------------------------------------------------------------------
// Curated arrays for common test scenarios
// -----------------------------------------------------------------------------

/** All ten bookings. */
export const MOCK_BOOKINGS: Booking[] = [
	MOCK_BOOKING_1, MOCK_BOOKING_2, MOCK_BOOKING_3, MOCK_BOOKING_4, MOCK_BOOKING_5,
	MOCK_BOOKING_6, MOCK_BOOKING_7, MOCK_BOOKING_8, MOCK_BOOKING_9, MOCK_BOOKING_10,
];

/** User 100's bookings only — matches the response shape of GET /api/bookings/me. */
export const MOCK_MY_BOOKINGS: Booking[] = [
	MOCK_BOOKING_1, MOCK_BOOKING_2, MOCK_BOOKING_3,
	MOCK_BOOKING_4, MOCK_BOOKING_5, MOCK_BOOKING_6,
	MOCK_BOOKING_7, MOCK_BOOKING_10,
];

/** Room 1's bookings on 2026-07-25 — matches GET /api/rooms/1/bookings?date=2026-07-25. */
export const MOCK_ROOM_1_BOOKINGS_JUL_25: Booking[] = [
	MOCK_BOOKING_8, MOCK_BOOKING_9,
];

/** Empty array — for testing "no bookings" states. */
export const MOCK_NO_BOOKINGS: Booking[] = [];
