package com.geoffvargo.gvorbabackend.security;

import java.time.*;

/**
 * Published by TwoFactorService for every security-relevant change. OWASP's MFA cheat sheet asks for an
 * out-of-band notice when a factor changes and when a second factor fails after a correct password
 * (that failure means the password is probably compromised).
 */
public record TwoFactorEvent(String username, Type type, Instant at) {
	public enum Type {
		ENABLED,
		DISABLED,
		VERIFICATION_FAILED,
		RECOVERY_CODE_USED
	}
}
