package com.geoffvargo.gvorbabackend.services.impl;

import com.geoffvargo.gvorbabackend.exceptions.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.services.RefreshTokenService.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.test.util.*;

import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RefreshTokenServiceImpl}. The repository is mocked, so these tests check what the service
 * hands to the repository and how it reacts to stored tokens in each state (missing, revoked, expired, valid).
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {
	private static final int TTL_DAYS = 7;
	private static final String RAW_TOKEN = "presented-raw-token";

	/// 32 random bytes, Base64 URL-encoded without padding, is always 43 characters.
	private static final String RAW_TOKEN_PATTERN = "^[A-Za-z0-9_-]{43}$";

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Captor
	private ArgumentCaptor<RefreshToken> savedCaptor;

	private RefreshTokenServiceImpl service;
	private User user;

	@BeforeEach
	void setUp() {
		service = new RefreshTokenServiceImpl(refreshTokenRepository);
		/// ttlDays is normally injected by @Value, so set it by hand here.
		ReflectionTestUtils.setField(service, "ttlDays", TTL_DAYS);
		user = buildUser(false);
	}

	/// --- issue(User) ---

	@Test
	void issue_returnsUrlSafe43CharToken() throws Exception {
		String raw = service.issue(user);

		assertTrue(raw.matches(RAW_TOKEN_PATTERN), () -> "unexpected token format: " + raw);
	}

	@Test
	void issue_returnsDifferentTokenOnEachCall() throws Exception {
		Set<String> tokens = new HashSet<>();
		for (int i = 0; i < 50; i++) {
			tokens.add(service.issue(user));
		}

		assertEquals(50, tokens.size());
	}

	@Test
	void issue_savesSha256HashOfRawToken_neverTheRawToken() throws Exception {
		String raw = service.issue(user);

		verify(refreshTokenRepository).save(savedCaptor.capture());
		RefreshToken saved = savedCaptor.getValue();

		assertEquals(sha256Hex(raw), saved.getTokenHash());
		assertNotEquals(raw, saved.getTokenHash());
	}

	@Test
	void issue_savesTokenForGivenUser() throws Exception {
		service.issue(user);

		verify(refreshTokenRepository).save(savedCaptor.capture());
		assertSame(user, savedCaptor.getValue().getUser());
	}

	@Test
	void issue_setsExpiryTtlDaysFromNow() throws Exception {
		LocalDateTime before = LocalDateTime.now();
		service.issue(user);
		LocalDateTime after = LocalDateTime.now();

		verify(refreshTokenRepository).save(savedCaptor.capture());
		assertExpiresTtlDaysAfter(before, after, savedCaptor.getValue().getExpiresAt());
	}

	@Test
	void issue_newTokenIsNotRevoked() throws Exception {
		service.issue(user);

		verify(refreshTokenRepository).save(savedCaptor.capture());
		assertNull(savedCaptor.getValue().getRevokedAt());
	}

	/// The mfa_verified column is NOT NULL, so a null here would fail on insert.
	@Test
	void issue_withoutMfaFlag_savesMfaVerifiedFalse() throws Exception {
		service.issue(user);

		verify(refreshTokenRepository).save(savedCaptor.capture());
		assertEquals(Boolean.FALSE, savedCaptor.getValue().getMfaVerified());
	}

	/// --- issue(User, boolean) ---

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void issueWithMfa_savesGivenMfaFlag(boolean mfaVerified) throws Exception {
		service.issue(user, mfaVerified);

		verify(refreshTokenRepository).save(savedCaptor.capture());
		assertEquals(mfaVerified, savedCaptor.getValue().getMfaVerified());
	}

	@Test
	void issueWithMfa_savesHashUserAndExpiry() throws Exception {
		LocalDateTime before = LocalDateTime.now();
		String raw = service.issue(user, true);
		LocalDateTime after = LocalDateTime.now();

		verify(refreshTokenRepository).save(savedCaptor.capture());
		RefreshToken saved = savedCaptor.getValue();

		assertTrue(raw.matches(RAW_TOKEN_PATTERN));
		assertEquals(sha256Hex(raw), saved.getTokenHash());
		assertSame(user, saved.getUser());
		assertNull(saved.getRevokedAt());
		assertExpiresTtlDaysAfter(before, after, saved.getExpiresAt());
	}

	/// --- rotate ---

	@Test
	void rotate_looksUpBySha256HashOfPresentedToken() {
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

		assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(RAW_TOKEN));

		verify(refreshTokenRepository).findByTokenHash(sha256Hex(RAW_TOKEN));
	}

	@Test
	void rotate_unknownToken_throwsAndWritesNothing() {
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

		InvalidRefreshTokenException ex =
			assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(RAW_TOKEN));

		assertEquals("No token found!", ex.getMessage());
		verify(refreshTokenRepository, never()).save(any());
		verify(refreshTokenRepository, never()).revokeAllForUser(any(), any());
	}

	@Test
	void rotate_revokedToken_revokesEveryTokenForUserAndThrows() {
		RefreshToken row = storedToken(LocalDateTime.now().plusDays(1), LocalDateTime.now().minusMinutes(1), false);
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(row));

		LocalDateTime before = LocalDateTime.now();
		InvalidRefreshTokenException ex =
			assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(RAW_TOKEN));
		LocalDateTime after = LocalDateTime.now();

		assertEquals("Token already revoked!", ex.getMessage());

		ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(refreshTokenRepository).revokeAllForUser(nowCaptor.capture(), eq(user.getId()));
		assertBetween(before, after, nowCaptor.getValue());

		/// A replayed token must never produce a fresh one.
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void rotate_revokedAndExpiredToken_stillTreatedAsReplay() {
		RefreshToken row = storedToken(LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(2), false);

		// TODO(human): stub the repository, call rotate, and assert what should happen when a token is BOTH revoked
		//  and expired.
	}

	@Test
	void rotate_expiredToken_throwsWithoutRevokingOrIssuing() {
		RefreshToken row = storedToken(LocalDateTime.now().minusMinutes(1), null, false);
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(row));

		InvalidRefreshTokenException ex =
			assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(RAW_TOKEN));

		assertEquals("Token has already expired!", ex.getMessage());
		assertNull(row.getRevokedAt());
		verify(refreshTokenRepository, never()).revokeAllForUser(any(), any());
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void rotate_validToken_returnsNewRawTokenWhoseHashIsSaved() {
		RefreshToken row = storedToken(LocalDateTime.now().plusDays(1), null, false);
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(row));

		RotationResult result = service.rotate(RAW_TOKEN);

		assertTrue(result.rawToken().matches(RAW_TOKEN_PATTERN));
		assertNotEquals(RAW_TOKEN, result.rawToken());

		verify(refreshTokenRepository, times(1)).save(savedCaptor.capture());
		RefreshToken saved = savedCaptor.getValue();
		assertNotSame(row, saved);
		assertEquals(sha256Hex(result.rawToken()), saved.getTokenHash());
		assertSame(user, saved.getUser());
	}

	@Test
	void rotate_validToken_revokesPresentedRow() {
		RefreshToken row = storedToken(LocalDateTime.now().plusDays(1), null, false);
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(row));

		LocalDateTime before = LocalDateTime.now();
		service.rotate(RAW_TOKEN);
		LocalDateTime after = LocalDateTime.now();

		assertBetween(before, after, row.getRevokedAt());
		verify(refreshTokenRepository, never()).revokeAllForUser(any(), any());
	}

	@Test
	void rotate_validToken_newTokenGetsFreshExpiry() {
		/// The old row expires in one hour; the new one should get the full TTL, not inherit that.
		RefreshToken row = storedToken(LocalDateTime.now().plusHours(1), null, false);
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(row));

		LocalDateTime before = LocalDateTime.now();
		service.rotate(RAW_TOKEN);
		LocalDateTime after = LocalDateTime.now();

		verify(refreshTokenRepository).save(savedCaptor.capture());
		assertExpiresTtlDaysAfter(before, after, savedCaptor.getValue().getExpiresAt());
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void rotate_validToken_carriesMfaVerifiedForward(boolean mfaVerified) {
		RefreshToken row = storedToken(LocalDateTime.now().plusDays(1), null, mfaVerified);
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(row));

		service.rotate(RAW_TOKEN);

		verify(refreshTokenRepository).save(savedCaptor.capture());
		assertEquals(mfaVerified, savedCaptor.getValue().getMfaVerified());
	}

	@Test
	void rotate_validToken_returnsPrincipalForTokenOwner() {
		RefreshToken row = storedToken(LocalDateTime.now().plusDays(1), null, false);
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(row));

		RotationResult result = service.rotate(RAW_TOKEN);

		assertEquals(user.getId(), result.principal().getId());
		assertEquals(user.getName(), result.principal().getUsername());
	}

	/// RotationResult.mfaVerified feeds LoginResponse.mfaRequired, so it mirrors the user's multifactor preference.
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void rotate_validToken_resultMfaFlagMirrorsUserMultifactorPref(boolean multifactor) {
		user = buildUser(multifactor);
		RefreshToken row = storedToken(LocalDateTime.now().plusDays(1), null, false);
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(row));

		RotationResult result = service.rotate(RAW_TOKEN);

		assertEquals(multifactor, result.mfaVerified());
	}

	@Test
	void rotationResult_toStringDoesNotLeakRawToken() {
		RefreshToken row = storedToken(LocalDateTime.now().plusDays(1), null, false);
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(row));

		RotationResult result = service.rotate(RAW_TOKEN);

		assertFalse(result.toString().contains(result.rawToken()));
	}

	/// --- revoke ---

	@Test
	void revoke_knownToken_setsRevokedAtToNow() {
		RefreshToken row = storedToken(LocalDateTime.now().plusDays(1), null, false);
		when(refreshTokenRepository.findByTokenHash(sha256Hex(RAW_TOKEN))).thenReturn(Optional.of(row));

		LocalDateTime before = LocalDateTime.now();
		service.revoke(RAW_TOKEN);
		LocalDateTime after = LocalDateTime.now();

		assertBetween(before, after, row.getRevokedAt());
	}

	@Test
	void revoke_knownToken_onlyRevokesThatToken() {
		RefreshToken row = storedToken(LocalDateTime.now().plusDays(1), null, false);
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(row));

		service.revoke(RAW_TOKEN);

		/// Logout relies on dirty checking; it must not save a new row or kill the user's other sessions.
		verify(refreshTokenRepository, never()).save(any());
		verify(refreshTokenRepository, never()).revokeAllForUser(any(), any());
	}

	@Test
	void revoke_unknownToken_isSilentNoOp() {
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

		assertDoesNotThrow(() -> service.revoke(RAW_TOKEN));

		verify(refreshTokenRepository).findByTokenHash(sha256Hex(RAW_TOKEN));
		verifyNoMoreInteractions(refreshTokenRepository);
	}

	/// --- purgeExpired ---

	@Test
	void purgeExpired_deletesTokensExpiringBeforeNow() {
		LocalDateTime before = LocalDateTime.now();
		service.purgeExpired();
		LocalDateTime after = LocalDateTime.now();

		ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(refreshTokenRepository).deleteByExpiresAtBefore(cutoffCaptor.capture());
		assertBetween(before, after, cutoffCaptor.getValue());
		verifyNoMoreInteractions(refreshTokenRepository);
	}

	/// --- helpers ---

	/**
	 * Builds a test user with a role and prefs, both of which {@code UserDetailsImpl.build} needs.
	 *
	 * @param multifactor whether the user has MFA turned on
	 * @return a user with id 1
	 */
	private static User buildUser(boolean multifactor) {
		User u = User.builder()
			         .id(1L)
			         .name("jane")
			         .email("jane@example.com")
			         .password("hashed")
			         .role(new Role(AppRole.ROLE_USER))
			         .build();
		u.setUserPrefs(UserPrefs.builder().user(u).multifactor(multifactor).build());
		return u;
	}

	/**
	 * Builds a stored token row owned by the current test user.
	 *
	 * @param expiresAt   when the token expires
	 * @param revokedAt   when it was revoked, or null if still active
	 * @param mfaVerified whether the session behind it passed MFA
	 * @return the row
	 */
	private RefreshToken storedToken(LocalDateTime expiresAt, LocalDateTime revokedAt, boolean mfaVerified) {
		return RefreshToken.builder()
			       .user(user)
			       .tokenHash(sha256Hex(RAW_TOKEN))
			       .expiresAt(expiresAt)
			       .revokedAt(revokedAt)
			       .mfaVerified(mfaVerified)
			       .build();
	}

	/**
	 * Computes the SHA-256 hex digest independently of the class under test, to pin the storage format.
	 *
	 * @param input the raw token
	 * @return lowercase hex digest
	 */
	private static String sha256Hex(String input) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Asserts an expiry is {@link #TTL_DAYS} after a moment captured between {@code before} and {@code after}.
	 */
	private static void assertExpiresTtlDaysAfter(LocalDateTime before, LocalDateTime after, LocalDateTime expiresAt) {
		assertNotNull(expiresAt);
		assertBetween(before.plusDays(TTL_DAYS), after.plusDays(TTL_DAYS), expiresAt);
	}

	/**
	 * Asserts {@code actual} falls within {@code [lower, upper]}, inclusive.
	 */
	private static void assertBetween(LocalDateTime lower, LocalDateTime upper, LocalDateTime actual) {
		assertNotNull(actual);
		assertFalse(actual.isBefore(lower), () -> actual + " is before " + lower);
		assertFalse(actual.isAfter(upper), () -> actual + " is after " + upper);
	}
}