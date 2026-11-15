-- totp_secret and totp_last_step were added in V11; this only adds the enrollment flag.
ALTER TABLE users
	ADD COLUMN totp_enrolled BOOLEAN NOT NULL DEFAULT FALSE;