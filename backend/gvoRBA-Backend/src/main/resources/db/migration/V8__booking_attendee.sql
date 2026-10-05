CREATE TABLE booking_attendee
(
	booking_id BIGINT NOT NULL,
	user_id    BIGINT NOT NULL,
	CONSTRAINT pk_booking_attendee PRIMARY KEY (booking_id, user_id),
	CONSTRAINT fk_booking_attendee_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
	CONSTRAINT fk_booking_attendee_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_booking_attendee_user_id ON booking_attendee (user_id);
