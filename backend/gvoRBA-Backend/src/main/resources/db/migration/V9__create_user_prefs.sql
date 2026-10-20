CREATE SEQUENCE IF NOT EXISTS user_prefs_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE user_prefs
(
	id                     BIGINT  NOT NULL,
	user_id                BIGINT  NOT NULL,
	email_notification     BOOLEAN NOT NULL DEFAULT FALSE,
	multifactor            BOOLEAN NOT NULL DEFAULT FALSE,
	confirm_bookings       BOOLEAN NOT NULL DEFAULT FALSE,
	notify_on_admin_cancel BOOLEAN NOT NULL DEFAULT FALSE,
	CONSTRAINT pk_user_prefs PRIMARY KEY (id),
	CONSTRAINT uc_user_prefs_user UNIQUE (user_id),
	CONSTRAINT fk_user_prefs_on_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE user_prefs_room_ids
(
	owner_id BIGINT NOT NULL,
	room_id  BIGINT NOT NULL,
	CONSTRAINT pk_user_prefs_room_ids PRIMARY KEY (owner_id, room_id),
	CONSTRAINT fk_user_prefs_room_ids_on_user_prefs FOREIGN KEY (owner_id) REFERENCES user_prefs (id) ON DELETE CASCADE,
	CONSTRAINT fk_user_prefs_room_ids_on_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
);

-- Give every existing user a default prefs row
INSERT INTO user_prefs (id, user_id)
SELECT nextval('user_prefs_seq'), u.id
FROM users u;