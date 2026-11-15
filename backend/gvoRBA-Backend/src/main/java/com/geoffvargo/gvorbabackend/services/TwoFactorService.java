package com.geoffvargo.gvorbabackend.services;

import com.geoffvargo.gvorbabackend.exceptions.*;
import com.geoffvargo.gvorbabackend.models.DTOs.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.security.*;
import com.geoffvargo.gvorbabackend.security.jwt.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.util.*;

import javax.annotation.*;

import lombok.*;
import lombok.extern.slf4j.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorService {
	private static final int RECOVERY_CODE_COUNT = 10;
	
	private static final int RECOVERY_CODE_LENGTH = 16;
	
	private static final String RECOVERY_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ23456789";
	
	private final UserRepository userRepository;
	
	private final RecoveryCodeRepository recoveryCodes;
	
	private final RefreshTokenService refreshTokens;
	
	private final JwtUtils jwtUtils;
	
	private final TotpService totpService;
	
	private final SecretCipher cipher;
	
	private final AttemptLimiter limiter;
	
	private final ApplicationEventPublisher events;
	
	private final Clock clock;
	
	@Value("${app.mfa.issuer}")
	private final String issuer;
	
	private final SecureRandom random = new SecureRandom();
	
	/**
	 * Accepts a TOTP code only if it matches a time step in the drift window AND that step was
	 * never used before; otherwise records a failure and throws.
	 * <p>
	 * {@link UserRepository#claimTotpStep} clears the persistence context, so callers must re-load
	 * the user before modifying it.
	 *
	 * @param user
	 * 	the user whose stored secret the code is checked against
	 * @param code
	 * 	the 6-digit code the user typed
	 * @throws TwoFactorException
	 * 	with {@code INVALID_OTP} and 401 if the code is wrong or was
	 * 	already used
	 */
	private void requireValidCode(User user, String code) {
		byte[] secret = cipher.decrypt(user.getTotpSecret());
		
		OptionalLong mstep = totpService.matchStep(secret, code);
		
		if (mstep.isPresent() && userRepository.claimTotpStep(user.getId(), mstep.getAsLong()) == 1) {
			return;
		}
		
		limiter.recordFailure(user.getName());
		events.publishEvent(
			new TwoFactorEvent(
				user.getName(),
				TwoFactorEvent.Type.VERIFICATION_FAILED,
				Instant.now())
		);
		
		throw new TwoFactorException(ErrorCode.INVALID_OTP, HttpStatus.UNAUTHORIZED);
	}
	
	/**
	 * Replaces the caller's session with one that has proven the second factor.
	 * <p>
	 * Revokes the refresh token the browser presented, issues a refresh token flagged
	 * {@code mfaVerified}, and creates an access token that carries {@link Factors#TOTP}.
	 *
	 * @param user
	 * 	the user who just passed the second factor
	 * @param refreshToken
	 * 	the raw refresh token from the request's cookie, or
	 *   {@code null} if the request had none
	 * @return the new login response and the new raw refresh token
	 */
	private IssuedSession upgradeSession(User user, @Nullable String refreshToken) {
		if (refreshToken != null) {
			refreshTokens.revoke(refreshToken);
		}
		
		try {
			refreshToken = refreshTokens.issue(user, true);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		
		UserDetailsImpl principal = UserDetailsImpl.build(user);
		String accessToken = jwtUtils.generateTokenFromUsername(principal, true);
		List<String> roles = principal.getAuthorities().stream().map(Object::toString).toList();
		LoginResponse response = new LoginResponse(
			accessToken,
			user.getName(),
			roles,
			false
		);
		
		return new IssuedSession(response, refreshToken);
	}
	
	/**
	 * Deletes the user's recovery codes and creates {@link #RECOVERY_CODE_COUNT} new ones.
	 * <p>
	 * Only the SHA-256 hash of each code is stored, so the returned list is the only time the
	 * plain codes exist. The caller shows them to the user once.
	 *
	 * @param user
	 * 	the user who gets the new codes
	 * @return the new codes, each formatted {@code XXXX-XXXX-XXXX-XXXX}
	 */
	private List<String> replaceRecoveryCodes(User user) {
		recoveryCodes.deleteAllForUser(user.getId());
		List<String> ans = new ArrayList<>();
		
		for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
			String recoveryCode = randomRecoveryChars();
			ans.add(recoveryCode);
		}
		
		ans.forEach(a -> {
			RecoveryCode r = RecoveryCode.builder()
				                 .user(user)
				                 .codeHash(sha256hex(a))
				                 .build();
			recoveryCodes.save(r);
		});
		
		return ans.stream().map(this::formatCode).toList();
	}
	
	/**
	 * Hashes a recovery code for storage and lookup.
	 *
	 * @param code
	 * 	the normalized recovery code
	 * @return the SHA-256 digest of {@code value}, as lowercase hexadecimal
	 */
	private static String sha256hex(String code) {
		byte[] ans;
		
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			ans = md.digest(code.getBytes(StandardCharsets.US_ASCII));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		
		return HexFormat.of().formatHex(ans);
	}
	
	private String formatCode(String code) {
		return code.replaceAll("(\\w{4})(\\w{4})(\\w{4})(\\w{4})", "$1-$2-$3-$4");
	}
	
	/**
	 * Returns a randomized 16 character string composed of characters from RECOVERY_ALPHABET
	 *
	 */
	private String randomRecoveryChars() {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < RECOVERY_CODE_LENGTH; i++) {
			str.insert(i, RECOVERY_ALPHABET.charAt(random.nextInt(RECOVERY_ALPHABET.length())));
		}
		return str.toString();
	}
	
	/**
	 * Puts a recovery code into the form its hash was computed from.
	 * <p>
	 * Accepts {@code abcd-efgh-...}, {@code ABCD EFGH ...} and similar input.
	 *
	 * @param input
	 * 	the recovery code as the user typed it
	 * @return the code's ASCII letters and digits only, in upper case
	 */
	private static String normalizeRecoveryCode(String input) {
		return input.replaceAll("[^A-Za-z0-9]", "");
	}
	
	private User loadUser(String username) {
		return userRepository.findByName(username).orElseThrow(
			() -> new TwoFactorException(ErrorCode.AUTHENTICATION_REQUIRED, HttpStatus.UNAUTHORIZED)
		);
	}
	
	/**
	 * Reports whether a user has two-factor enabled.
	 *
	 * @param username
	 * 	the signed-in user's name
	 * @return the user's two-factor status
	 * @throws TwoFactorException
	 * 	if the user doesn't exist
	 */
	@Transactional(readOnly = true)
	public StatusResponse status(String username) {
		return new StatusResponse(loadUser(username).isTotpEnrolled());
	}
	
	/**
	 * Enrollment part 1: creates and stores a new TOTP secret, but does NOT enable two-factor yet.
	 *
	 * @param username
	 * 	the signed-in user's name
	 * @return the {@code otpauth://} URI for the QR code and the secret in Base32
	 * @throws TwoFactorException
	 * 	with {@code MFA_ALREADY_ENABLED} and 409 if two-factor is already
	 * 	enabled, or with {@code AUTHENTICATION_REQUIRED} and 401 if the
	 * 	user doesn't exist
	 */
	@Transactional
	public SetupResponse setup(String username) {
		User user = loadUser(username);
		
		byte[] secret = totpService.newSecret();
		user.setTotpSecret(cipher.encrypt(secret));
		user.setTotpLastStep(null);
		
		return new SetupResponse(
			totpService.otpAuthUri(issuer, username, secret),
			totpService.toBase32(secret)
		);
	}
	
	/**
	 * Enrollment part 2: the first valid code proves the authenticator app holds the secret, so
	 * two-factor is enabled and recovery codes are created.
	 * <p>
	 * Enabling before this check would lock out a user whose QR scan failed.
	 *
	 * @param username
	 * 	the signed-in user's name
	 * @param code
	 * 	the 6-digit code from the authenticator app
	 * @param refreshToken
	 * 	the raw refresh token from the request's cookie, or
	 *   {@code null} if the request had none
	 * @return the recovery codes to show once, and the new verified session
	 * @throws TwoFactorException
	 * 	if too many attempts failed, two-factor is already enabled, setup
	 * 	wasn't started, or the code is wrong or already used
	 */
	@Transactional
	public ConfirmResult confirm(String username, String code, @Nullable String refreshToken) {
		limiter.checkAllowed(username);
		
		User user = loadUser(username);
		
		if (user.getUserPrefs().getMultifactor()) {
			throw new TwoFactorException(ErrorCode.MFA_ALREADY_ENABLED, HttpStatus.CONFLICT);
		}
		
		if (user.getTotpSecret() == null) {
			throw new TwoFactorException(ErrorCode.MFA_SETUP_NOT_STARTED, HttpStatus.CONFLICT);
		}
		
		requireValidCode(user, code);
		
		user = loadUser(username); /// make sure our copy of user isn't stale
		user.setTotpEnrolled(true);   /// mark setup as complete
		
		List<String> recoveryCodes = replaceRecoveryCodes(user);
		
		events.publishEvent(
			new TwoFactorEvent(
				username,
				TwoFactorEvent.Type.ENABLED,
				Instant.now()
			)
		);
		
		return new ConfirmResult(
			recoveryCodes,
			upgradeSession(user, refreshToken)
		);
	}
	
	/**
	 * Upgrades a pending session to a verified one with a valid code (new access token AND new
	 * refresh token).
	 *
	 * @param username
	 * 	the signed-in user's name
	 * @param code
	 * 	the 6-digit code from the authenticator app
	 * @param refreshToken
	 * 	the raw refresh token from the request's cookie, or
	 *   {@code null} if the request had none
	 * @return the new verified session
	 * @throws TwoFactorException
	 * 	if too many attempts failed, the user isn't enrolled, or the code
	 * 	is wrong or already used
	 */
	@Transactional
	public IssuedSession verify(String username, String code, @Nullable String refreshToken) {
		limiter.checkAllowed(username);
		
		User user = loadUser(username);
		
		if (user.isTotpEnrolled()) {
			throw new TwoFactorException(ErrorCode.MFA_NOT_ENABLED, HttpStatus.CONFLICT);
		}
		
		requireValidCode(user, code);
		
		user = loadUser(username); /// make sure our copy of user isn't stale
		
		return upgradeSession(user, refreshToken);
	}
	
	/**
	 * Upgrades a pending session to a verified one with an unused recovery code, and burns the code
	 * atomically.
	 * <p>
	 * Simplification: a recovery code counts as {@link Factors#TOTP}.
	 *
	 * @param username
	 * 	the signed-in user's name
	 * @param recoveryCode
	 * 	the recovery code as the user typed it
	 * @param refreshToken
	 * 	the raw refresh token from the request's cookie, or
	 *   {@code null} if the request had none
	 * @return the new verified session
	 * @throws TwoFactorException
	 * 	if too many attempts failed, the user isn't enrolled, or the
	 * 	recovery code is wrong or already used
	 */
	@Transactional
	public IssuedSession recover(String username, String recoveryCode, @Nullable String refreshToken) {
		limiter.checkAllowed(username);
		
		User user = loadUser(username);
		
		if (!user.isTotpEnrolled()) {
			throw new TwoFactorException(ErrorCode.MFA_NOT_ENABLED, HttpStatus.CONFLICT);
		}
		
		String hash = sha256hex(normalizeRecoveryCode(recoveryCode));
		
		switch (recoveryCodes.claim(user.getId(), hash, LocalDateTime.now())) {
			case 0:
				limiter.recordFailure(username);
				events.publishEvent(
					new TwoFactorEvent(username, TwoFactorEvent.Type.VERIFICATION_FAILED, clock.instant()));
				throw new TwoFactorException(ErrorCode.INVALID_RECOVERY_CODE, HttpStatus.UNAUTHORIZED);
			case 1:
				limiter.reset(username);
				events.publishEvent(
					new TwoFactorEvent(username, TwoFactorEvent.Type.RECOVERY_CODE_USED, clock.instant()));
				user = loadUser(username);
				break;
		}
		
		return upgradeSession(user, refreshToken);
	}
	
	/**
	 * Removes the user's authenticator enrollment: deletes the secret and recovery codes and marks the
	 * user not enrolled. Doesn't touch the user's {@code multifactor} preference.
	 * <p>
	 * Requires a verified session (enforced in {@code SecurityConfig}) AND a fresh code, so a stolen
	 * token alone cannot disable two-factor.
	 *
	 * @param username the signed-in user's name
	 * @param code     a current 6-digit code from the authenticator app
	 * @throws TwoFactorException if too many attempts failed, the user isn't enrolled, or the code
	 *                            is wrong or already used
	 */
	@Transactional
	public void disable(String username, String code) {
		limiter.checkAllowed(username);
		
		User user = loadUser(username);
		
		if (user.isTotpEnrolled()) {
			throw new TwoFactorException(ErrorCode.MFA_NOT_ENABLED, HttpStatus.CONFLICT);
		}
		
		requireValidCode(user, code);
		
		user = loadUser(username);
		
		user.setTotpEnrolled(false);
		user.setTotpSecret(null);
		user.setTotpLastStep(null);
		
		recoveryCodes.deleteAllForUser(user.getId());
		
		events.publishEvent(
			new TwoFactorEvent(
				username,
				TwoFactorEvent.Type.DISABLED,
				clock.instant()
			)
		);
	}
}
