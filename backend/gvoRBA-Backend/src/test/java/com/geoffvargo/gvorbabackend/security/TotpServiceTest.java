package com.geoffvargo.gvorbabackend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.OptionalLong;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.geoffvargo.gvorbabackend.services.*;
import com.geoffvargo.gvorbabackend.services.impl.*;

/**
 * Unit tests for {@link TotpServiceImpl}.
 *
 * <p>The service runs on a clock fixed at {@link #NOW}, so the current time step never changes.
 * Expected codes come from the java-otp generator directly (see {@link #codeAt(Instant)}), which
 * is what a user's authenticator app would show.
 */
class TotpServiceTest {

	/**
	 * The fixed "current" time. It is 15 seconds into a 30-second step, so moving 30 seconds either
	 * way lands in the middle of the neighboring step, not on a step boundary.
	 */
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:15Z");

	/**
	 * The 30-second time step that contains {@link #NOW}.
	 */
	private static final long CURRENT_STEP = NOW.getEpochSecond() / 30;

	/**
	 * The service under test, using a clock fixed at {@link #NOW}.
	 */
	private final TotpService totp = new TotpServiceImpl(Clock.fixed(NOW, ZoneOffset.UTC));

	/**
	 * A new random secret for each test. Expected codes are generated from it, so its value does
	 * not affect any result.
	 */
	private final byte[] secret = totp.newSecret();

	/**
	 * Verifies that the code for {@link #NOW} matches {@link #CURRENT_STEP}.
	 *
	 * @throws Exception
	 * 	if the expected code cannot be generated
	 */
	@Test
	void currentCodeMatchesCurrentStep() throws Exception {
		assertEquals(OptionalLong.of(CURRENT_STEP), totp.matchStep(secret, codeAt(NOW)));
	}

	/**
	 * Verifies that codes from one step before and one step after {@link #NOW} are accepted and
	 * report their own step, allowing for drift between the server and phone clocks.
	 *
	 * @throws Exception
	 * 	if an expected code cannot be generated
	 */
	@Test
	void codesOneStepEitherSideAreAcceptedForClockDrift() throws Exception {
		assertEquals(OptionalLong.of(CURRENT_STEP - 1), totp.matchStep(secret, codeAt(NOW.minusSeconds(30))));
		assertEquals(OptionalLong.of(CURRENT_STEP + 1), totp.matchStep(secret, codeAt(NOW.plusSeconds(30))));
	}

	/**
	 * Verifies that a code two steps from {@link #NOW}, outside the drift window, is rejected.
	 *
	 * @throws Exception
	 * 	if the expected code cannot be generated
	 */
	@Test
	void codeTwoStepsAwayIsRejected() throws Exception {
		assertTrue(totp.matchStep(secret, codeAt(NOW.plusSeconds(60))).isEmpty());
	}

	/**
	 * Verifies that input that is not a numeric code is rejected rather than causing an error.
	 */
	@Test
	void nonNumericInputIsRejected() {
		assertTrue(totp.matchStep(secret, "abcdef").isEmpty());
	}

	/**
	 * Verifies that the Base32 form of a 20-byte secret is 32 characters with no {@code =} padding,
	 * the form authenticator apps expect.
	 */
	@Test
	void base32SecretHasNoPadding() {
		String base32 = totp.toBase32(secret);
		assertEquals(32, base32.length());   /// 20 bytes = 160 bits = 32 Base32 characters
		assertFalse(base32.contains("="));
	}

	/**
	 * Verifies that the {@code otpauth://} URI contains the URL-encoded label, the Base32 secret and
	 * the {@code issuer} parameter, with a space encoded as {@code %20} and {@code @} as {@code %40}.
	 */
	@Test
	void otpAuthUriEncodesIssuerAndAccount() {
		String uri = totp.otpAuthUri("My App", "alice@example.com", secret);
		assertTrue(uri.startsWith("otpauth://totp/My%20App:alice%40example.com?secret=" + totp.toBase32(secret)));
		assertTrue(uri.contains("&issuer=My%20App"));
	}

	/**
	 * Generates the code a user's authenticator app would show at the given time.
	 *
	 * @param at
	 * 	the time to generate the code for
	 *
	 * @return the 6-digit code for {@link #secret} at {@code at}
	 *
	 * @throws Exception
	 * 	if {@link #secret} is rejected as an HMAC key
	 */
	private String codeAt(Instant at) throws Exception {
		TimeBasedOneTimePasswordGenerator generator = new TimeBasedOneTimePasswordGenerator();
		return generator.generateOneTimePasswordString(new SecretKeySpec(secret, generator.getAlgorithm()), at);
	}
}