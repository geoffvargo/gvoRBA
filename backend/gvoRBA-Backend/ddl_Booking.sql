CREATE TABLE bookings
(
    id        BIGINT NOT NULL,
    starts_at TIMESTAMP WITHOUT TIME ZONE,
    ends_at   TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_bookings PRIMARY KEY (id)
);

CREATE TABLE bookings
(
    id        BIGINT NOT NULL,
    room_id   BIGINT NOT NULL,
    starts_at TIMESTAMP WITHOUT TIME ZONE,
    ends_at   TIMESTAMP WITHOUT TIME ZONE,
    purpose   VARCHAR(255),
    status    SMALLINT,
    CONSTRAINT pk_bookings PRIMARY KEY (id)
);

ALTER TABLE bookings
    ADD CONSTRAINT FK_BOOKINGS_ON_ROOM FOREIGN KEY (room_id) REFERENCES rooms (id);
