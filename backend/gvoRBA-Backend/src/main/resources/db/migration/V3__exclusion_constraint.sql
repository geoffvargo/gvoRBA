ALTER TABLE bookings
	ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP,
	ADD COLUMN IF NOT EXISTS cancelled_by BIGINT REFERENCES users(id);

ALTER TABLE bookings
	ADD CONSTRAINT bookings_no_overlap
		EXCLUDE USING gist (
		room_id  WITH =,
		tsrange(starts_at, ends_at, '[)') WITH &&
		)
		WHERE (cancelled_at IS NULL);
