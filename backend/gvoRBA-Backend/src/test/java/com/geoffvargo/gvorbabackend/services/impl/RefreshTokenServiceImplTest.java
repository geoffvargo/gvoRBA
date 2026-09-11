package com.geoffvargo.gvorbabackend.services.impl;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.services.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Captor
	private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

	private RefreshTokenServiceImpl refreshTokenServiceImpl;
	private User user;

	@BeforeEach
	void setUp() {
		refreshTokenServiceImpl = new RefreshTokenServiceImpl(refreshTokenRepository);
		user = User.builder()
			       .id(1L)
			       .name("jane")
			       .email("jane@example.com")
			       .password("hashed")
			       .role(new Role(AppRole.ROLE_USER))
			       .build();
	}

	@Test
	void issue_returnsNonNullDigestString() throws Exception {
		String token = refreshTokenServiceImpl.issue(user);

		assertNotNull(token);
		assertFalse(token.isEmpty());
	}

	@Test
	void issue_returnsDifferentValueOnEachCall() throws Exception {
		String first = refreshTokenServiceImpl.issue(user);
		String second = refreshTokenServiceImpl.issue(user);

		assertNotEquals(first, second);
	}

	@Test
	void issue_doesNotThrow() {
		assertDoesNotThrow(() -> refreshTokenServiceImpl.issue(user));
	}

	@Test
	void issue_savesRefreshTokenForGivenUser() throws Exception {
		String token = refreshTokenServiceImpl.issue(user);

		verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
		RefreshToken saved = refreshTokenCaptor.getValue();

		assertEquals(user, saved.getUser());
		assertEquals(token, saved.getTokenHash());
	}

	@Test
	void issue_setsNonNullExpiresAt() throws Exception {
		refreshTokenServiceImpl.issue(user);

		verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

		assertNotNull(refreshTokenCaptor.getValue().getExpiresAt());
	}

	@Test
	void rotate_throwsWhenTokenNotFound() {
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

		RuntimeException ex = assertThrows(RuntimeException.class,
			() -> refreshTokenServiceImpl.rotate("some-raw-token"));

		assertEquals("No token found!", ex.getMessage());
	}

	@Test
	void rotate_throwsAndRevokesAllForUserWhenTokenAlreadyRevoked() {
		RefreshToken revoked = RefreshToken.builder()
			                       .user(user)
			                       .tokenHash("hash")
			                       .expiresAt(LocalDateTime.now().plusMinutes(5))
			                       .revokedAt(LocalDateTime.now().minusMinutes(1))
			                       .build();
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

		RuntimeException ex = assertThrows(RuntimeException.class,
			() -> refreshTokenServiceImpl.rotate("some-raw-token"));

		assertEquals("Token already revoked!", ex.getMessage());
		verify(refreshTokenRepository).revokeAllForUser(any(LocalDateTime.class), eq(user.getId()));
	}

	@Test
	void rotate_throwsWhenTokenExpired() {
		RefreshToken expired = RefreshToken.builder()
			                       .user(user)
			                       .tokenHash("hash")
			                       .expiresAt(LocalDateTime.now().minusMinutes(1))
			                       .build();
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

		RuntimeException ex = assertThrows(RuntimeException.class,
			() -> refreshTokenServiceImpl.rotate("some-raw-token"));

		assertEquals("Token has already expired!", ex.getMessage());
	}

	@Test
	void rotate_returnsRotationResultAndRevokesOldTokenWhenValid() {
		RefreshToken valid = RefreshToken.builder()
			                     .user(user)
			                     .tokenHash("hash")
			                     .expiresAt(LocalDateTime.now().plusMinutes(5))
			                     .build();
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(valid));

		RefreshTokenService.RotationResult result = refreshTokenServiceImpl.rotate("some-raw-token");

		assertNotNull(result);
		assertNotNull(result.rawToken());
		assertFalse(result.rawToken().isEmpty());
		assertEquals(user.getId(), result.principal().getId());
		assertNotNull(valid.getRevokedAt());
	}

	@Test
	void rotate_issuesAndSavesNewTokenWhenValid() {
		RefreshToken valid = RefreshToken.builder()
			                     .user(user)
			                     .tokenHash("hash")
			                     .expiresAt(LocalDateTime.now().plusMinutes(5))
			                     .build();
		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(valid));

		refreshTokenServiceImpl.rotate("some-raw-token");

		verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
		assertEquals(user, refreshTokenCaptor.getValue().getUser());
	}

	@Test
	void revoke_doesNotThrow() {
		assertDoesNotThrow(() -> refreshTokenServiceImpl.revoke("some-raw-token"));
	}

	@Test
	void purgeExpired_doesNotThrow() {
		assertDoesNotThrow(() -> refreshTokenServiceImpl.purgeExpired());
	}
}