CREATE SEQUENCE IF NOT EXISTS auth_handoff_code_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE auth_handoff_code
(
	id          BIGINT NOT NULL,
	user_id     BIGINT,
	code_hash   VARCHAR(255),
	expires_at  TIMESTAMP(6) WITHOUT TIME ZONE,
	consumed_at TIMESTAMP(6) WITHOUT TIME ZONE,
	CONSTRAINT pk_auth_handoff_code PRIMARY KEY (id)
);

ALTER TABLE auth_handoff_code
	ADD CONSTRAINT FK_AUTH_HANDOFF_CODE_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);
