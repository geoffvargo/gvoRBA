package com.geoffvargo.gvorbabackend.services.impl;

import com.eatthepath.otp.*;
import com.geoffvargo.gvorbabackend.services.*;

import org.apache.commons.codec.binary.*;
import org.springframework.stereotype.*;

import java.net.*;
import java.security.*;
import java.time.*;
import java.util.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import lombok.*;

import static java.nio.charset.StandardCharsets.*;

@Service
@RequiredArgsConstructor
public class TotpServiceImpl implements TotpService {
	/**
	 * Accept codes from one step before and one step after "now" (±30 s of phone clock drift).
	 */
	public static final int DRIFT_STEPS = 1;
	
	/**
	 * 160 bits = HMAC-SHA1 output length, the size RFC 4226 recommends and authenticator apps expect.
	 */
	private static final int SECRET_BYTES = 20;
	
	/// Thread-safe and reusable per the java-otp README. Defaults: 6 digits, 30 s step, HmacSHA1.
	private final TimeBasedOneTimePasswordGenerator generator = new TimeBasedOneTimePasswordGenerator();
	private final SecureRandom random = new SecureRandom();
	private final Clock clock;
	
	@Override
	public byte[] newSecret() {
		byte[] secret = new byte[SECRET_BYTES];
		random.nextBytes(secret);
		return secret;
	}
	
	/**
	 * Base32 without '=' padding: the form authenticator apps expect for manual entry and in the URI.
	 */
	@Override
	public String toBase32(byte[] secret) {
		return new Base32().encodeToString(secret).replace("=", "");
	}
	
	/**
	 * Builds the {@code otpauth://totp/} key URI that authenticator apps (Google Authenticator, Authy,
	 * 1Password, etc.) read to enroll a secret.
	 * <p>
	 * The frontend renders this string as a QR code. It contains the secret, so never send it to a
	 * third-party QR service. The {@code algorithm}, {@code digits} and {@code period} values match the
	 * generator's defaults; change them together.
	 *
	 * @param issuer      the service name shown in the app; used in the label and the {@code issuer} parameter
	 * @param accountName the user's identifier shown under the issuer, e.g. their email
	 * @param secret      the raw shared secret; written to the URI as unpadded Base32
	 *
	 * @return the URI, with the issuer and account name URL-encoded (spaces as {@code %20})
	 *
	 * @throws NullPointerException if any argument is {@code null}
	 */
	@Override
	public String otpAuthUri(String issuer, String accountName, byte[] secret) {
		String label = encode(issuer) + ":" + encode(accountName);
		
		return "otpauth://totp/" + label + "?secret=" + toBase32(secret)
		       + "&issuer=" + encode(issuer) + "&algorithm=SHA1&digits=6&period=30";
	}
	
	/**
	 * Checks a user-supplied TOTP code against the secret and reports which time step it belongs to.
	 * <p>
	 * The current step is {@code floor(epochSeconds / stepSeconds)}, read from the injected {@link Clock}.
	 * The expected code is computed for every step from {@code currentStep - DRIFT_STEPS} to
	 * {@code currentStep + DRIFT_STEPS} and compared with {@link MessageDigest#isEqual(byte[], byte[])},
	 * which runs in constant time. The loop never exits early, so response time does not reveal whether a
	 * code matched or which step it matched.
	 * <p>
	 * This method only checks that the code is correct. It does <em>not</em> stop replays: a valid code
	 * matches again on every call until its step leaves the drift window. The caller must record the
	 * returned step and reject any step at or before the last one used (see
	 * {@code AppUserRepository.claimTotpStep}).
	 *
	 * @param secret       the user's raw shared secret (not Base32), as produced by {@link #newSecret()}
	 * @param suppliedCode the code the user typed, e.g. {@code "123456"}; compared exactly, so trim
	 *                     whitespace and remove separators before calling
	 *
	 * @return the matched time step (seconds since the epoch divided by the step length), or
	 *        {@link OptionalLong#empty()} if the code matches no step in the drift window; if more than
	 *        one step produces the same code, the earliest one is returned
	 *
	 * @throws NullPointerException     if {@code suppliedCode} is {@code null}
	 * @throws IllegalArgumentException if {@code secret} is {@code null} or empty
	 * @throws IllegalStateException    if the secret is rejected as an HMAC key
	 * @see #DRIFT_STEPS
	 */
	@Override
	public OptionalLong matchStep(byte[] secret, String suppliedCode) {
		SecretKey key = new SecretKeySpec(secret, generator.getAlgorithm());
		long stepSeconds = generator.getTimeStep().toSeconds();
		long currentStep = clock.instant().getEpochSecond() / stepSeconds;
		byte[] supplied = suppliedCode.getBytes(US_ASCII);
		
		OptionalLong matched = OptionalLong.empty();
		/// No early return: every candidate is computed and compared, so timing does not reveal which matched.
		for (long step = currentStep - DRIFT_STEPS; step <= currentStep + DRIFT_STEPS; step++) {
			byte[] expected = codeAt(key, Instant.ofEpochSecond(step * stepSeconds)).getBytes(US_ASCII);
			if (MessageDigest.isEqual(expected, supplied) && matched.isEmpty()) {
				matched = OptionalLong.of(step);
			}
		}
		
		return matched;
	}
	
	private String codeAt(SecretKey key, Instant instant) {
		try {
			return generator.generateOneTimePasswordString(key, instant);
		} catch (InvalidKeyException e) {
			throw new IllegalStateException("TOTP secret is not a valid HMAC key.", e);
		}
	}
	
	private static String encode(String value) {
		return URLEncoder.encode(value, UTF_8).replace("+", "%20");
	}
}
