ALTER TABLE user_prefs
	ADD COLUMN confirm_cancellations      BOOLEAN NOT NULL DEFAULT FALSE,
	ADD COLUMN notify_on_booking_change   BOOLEAN NOT NULL DEFAULT FALSE,
	ADD COLUMN notify_on_room_deactivated BOOLEAN NOT NULL DEFAULT FALSE;