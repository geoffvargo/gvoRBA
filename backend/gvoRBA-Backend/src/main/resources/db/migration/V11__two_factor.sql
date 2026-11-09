-- Two-factor authentication (TOTP). PostgreSQL.

ALTER TABLE users
	ADD COLUMN totp_secret    VARCHAR(255), -- AES-GCM ciphertext, Base64. NULL = never set up
	ADD COLUMN totp_last_step BIGINT; -- last accepted TOTP time step (replay guard)

ALTER TABLE refresh_token
	ADD COLUMN mfa_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE SEQUENCE IF NOT EXISTS recovery_code_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE recovery_code
(
	id        BIGINT      NOT NULL,
	user_id   BIGINT      NOT NULL,
	code_hash VARCHAR(64) NOT NULL,           -- SHA-256 hex of an 80-bit random code
	used_at   TIMESTAMP(6) WITHOUT TIME ZONE, -- NULL = still usable
	CONSTRAINT pk_recovery_code PRIMARY KEY (id),
	CONSTRAINT fk_recvoery_code_on_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
	CONSTRAINT uq_recovery_code_user_hash UNIQUE (user_id, code_hash)
);
