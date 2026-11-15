package com.geoffvargo.gvorbabackend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import com.geoffvargo.gvorbabackend.models.RecoveryCode;
import com.geoffvargo.gvorbabackend.models.User;
import com.geoffvargo.gvorbabackend.repos.RecoveryCodeRepository;

/**
 * Unit tests for the recovery-code helpers in {@link TwoFactorService}.
 *
 * <p>The methods under test are private, so they are called through {@link ReflectionTestUtils}.
 * The only dependency they use is {@link RecoveryCodeRepository}, which is a Mockito mock; every
 * other constructor argument is passed as {@code null}.
 */
class TwoFactorServiceTest {

	/**
	 * Matches exactly 16 characters from {@code RECOVERY_ALPHABET} (A-Z and 2-9, with no 0 or 1).
	 */
	private static final String RECOVERY_CODE_PATTERN = "[A-Z2-9]{16}";

	/**
	 * Matches a recovery code as shown to the user: four groups of four alphabet characters.
	 */
	private static final String FORMATTED_CODE_PATTERN = "[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}";

	/**
	 * Mock repository, used to check which recovery codes are deleted and saved.
	 */
	private final RecoveryCodeRepository recoveryCodes = mock(RecoveryCodeRepository.class);

	/**
	 * The service under test.
	 */
	private final TwoFactorService service =
		new TwoFactorService(null, recoveryCodes, null, null, null, null, null, null, null, "test-issuer");

	/**
	 * The user whose codes are replaced in the {@code replaceRecoveryCodes} tests.
	 */
	private final User user = User.builder().id(42L).build();

	/**
	 * Verifies that a generated code is exactly 16 characters long.
	 */
	@Test
	void recoveryCodeHasExpectedLength() {
		String str = randomRecoveryChars();
		System.out.println(str);
		assertEquals(16, str.length());
	}

	/**
	 * Verifies that every character of many generated codes comes from {@code RECOVERY_ALPHABET}.
	 * Checking many codes makes it likely that every alphabet position is drawn at least once.
	 */
	@Test
	void recoveryCodeUsesOnlyAlphabetCharacters() {
		for (int i = 0; i < 1_000; i++) {
			String code = randomRecoveryChars();
			System.out.println(code);
			assertTrue(code.matches(RECOVERY_CODE_PATTERN), () -> "Unexpected recovery code: " + code);
		}
	}

	/**
	 * Verifies that the generator does not repeat codes.
	 */
	@Test
	void recoveryCodesAreUnique() {
		Set<String> codes = new HashSet<>();
		
		int size = 100_000;
		for (int i = 0; i < size; i++) {
			codes.add(randomRecoveryChars());
		}
		
		assertEquals(size, codes.size());
	}

	/**
	 * Verifies the hash against the published SHA-256 test vectors for {@code "abc"} and the empty
	 * string (FIPS 180-2), so the result is checked against a known answer rather than against
	 * another call to {@link java.security.MessageDigest}.
	 */
	@Test
	void sha256hexMatchesKnownTestVectors() {
		assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256hex("abc"));
		assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha256hex(""));
	}

	/**
	 * Verifies that the hash of a formatted recovery code is 64 lowercase hex characters
	 * (32 bytes), the form stored in {@code RecoveryCode.codeHash}.
	 */
	@Test
	void sha256hexReturns64LowercaseHexCharacters() {
		assertTrue(sha256hex("ABCD-EFGH-JKLM-NPQR").matches("[0-9a-f]{64}"));
	}

	/**
	 * Verifies that hashing the same code twice gives the same result, which is what lets a
	 * submitted code be matched against the stored hash.
	 */
	@Test
	void sha256hexIsDeterministic() {
		assertEquals(sha256hex("ABCD-EFGH-JKLM-NPQR"), sha256hex("ABCD-EFGH-JKLM-NPQR"));
	}

	/**
	 * Verifies that the hash is sensitive to dashes and letter case. A submitted code must be put
	 * into the stored raw form (16 uppercase characters, no dashes) before hashing, or it won't match.
	 */
	@Test
	void sha256hexIsSensitiveToFormatting() {
		String stored = sha256hex("ABCDEFGHJKLMNPQR");

		assertNotEquals(stored, sha256hex("ABCD-EFGH-JKLM-NPQR"));
		assertNotEquals(stored, sha256hex("abcdefghjklmnpqr"));
	}

	/**
	 * Verifies that 10 distinct codes are returned, each in {@code XXXX-XXXX-XXXX-XXXX} form.
	 */
	@Test
	void replaceRecoveryCodesReturnsTenFormattedDistinctCodes() {
		List<String> codes = replaceRecoveryCodes(user);

		assertEquals(10, codes.size());
		assertEquals(10, new HashSet<>(codes).size());
		codes.forEach(code ->
			assertTrue(code.matches(FORMATTED_CODE_PATTERN), () -> "Unexpected recovery code: " + code));
	}

	/**
	 * Verifies that the user's old codes are deleted before any new code is saved, so old and new
	 * codes are never both valid.
	 */
	@Test
	void replaceRecoveryCodesDeletesOldCodesBeforeSavingNewOnes() {
		replaceRecoveryCodes(user);

		InOrder inOrder = inOrder(recoveryCodes);
		inOrder.verify(recoveryCodes).deleteAllForUser(42L);
		inOrder.verify(recoveryCodes, times(10)).save(any(RecoveryCode.class));
	}

	/**
	 * Verifies that each saved row belongs to the user and holds the SHA-256 hash of one returned
	 * code in its raw form (the 16 characters without dashes), and that no plain code is stored.
	 */
	@Test
	void replaceRecoveryCodesStoresOnlyHashesOfReturnedCodes() {
		List<String> codes = replaceRecoveryCodes(user);
		List<String> rawCodes = codes.stream().map(code -> code.replace("-", "")).toList();
		List<RecoveryCode> saved = savedRecoveryCodes();

		List<String> expectedHashes = rawCodes.stream().map(TwoFactorServiceTest::sha256hex).toList();
		List<String> savedHashes = saved.stream().map(RecoveryCode::getCodeHash).toList();

		assertEquals(expectedHashes, savedHashes);
		saved.forEach(r -> {
			assertSame(user, r.getUser());
			assertFalse(codes.contains(r.getCodeHash()));
			assertFalse(rawCodes.contains(r.getCodeHash()));
		});
	}

	/**
	 * Verifies that newly created codes are unused, so the user can redeem them later.
	 * {@link RecoveryCodeRepository#claim} only accepts a code whose {@code usedAt} is {@code null}.
	 */
	@Test
	void replaceRecoveryCodesSavesUnusedCodes() {
		replaceRecoveryCodes(user);
		List<RecoveryCode> saved = savedRecoveryCodes();

		// TODO(human): assert that every saved code is still unused.
	}

	/**
	 * Returns every {@link RecoveryCode} passed to {@code recoveryCodes.save(...)}, in call order.
	 *
	 * @return the saved recovery codes
	 */
	private List<RecoveryCode> savedRecoveryCodes() {
		ArgumentCaptor<RecoveryCode> captor = ArgumentCaptor.forClass(RecoveryCode.class);
		verify(recoveryCodes, atLeastOnce()).save(captor.capture());
		return captor.getAllValues();
	}

	/**
	 * Calls the private {@code replaceRecoveryCodes(User)} method on {@link #service}.
	 *
	 * @param user
	 * 	the user whose codes are replaced
	 *
	 * @return the new plain codes
	 */
	private List<String> replaceRecoveryCodes(User user) {
		return ReflectionTestUtils.invokeMethod(service, "replaceRecoveryCodes", user);
	}

	/**
	 * Calls the private {@code randomRecoveryChars()} method on {@link #service}.
	 *
	 * @return a newly generated recovery code
	 */
	private String randomRecoveryChars() {
		return ReflectionTestUtils.invokeMethod(service, "randomRecoveryChars");
	}

	/**
	 * Calls the private static {@code sha256hex(String)} method on {@link TwoFactorService}.
	 *
	 * @param code
	 * 	the text to hash
	 *
	 * @return the SHA-256 hash of {@code code} as lowercase hex
	 */
	private static String sha256hex(String code) {
		return ReflectionTestUtils.invokeMethod(TwoFactorService.class, "sha256hex", code);
	}
}
