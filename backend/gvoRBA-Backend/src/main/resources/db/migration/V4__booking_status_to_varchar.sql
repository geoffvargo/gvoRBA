-- backend/gvoRBA-Backend/src/main/resources/db/migration/V4__booking_status_to_varchar.sql
ALTER TABLE bookings
	ALTER COLUMN status TYPE VARCHAR(255)
		USING (
		CASE status
			WHEN 0 THEN 'CONFIRMED'
			WHEN 1 THEN 'CANCELLED'
			END
		);
