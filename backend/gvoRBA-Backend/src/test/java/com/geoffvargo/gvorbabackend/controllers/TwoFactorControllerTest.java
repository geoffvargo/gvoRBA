package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.exceptions.*;
import com.geoffvargo.gvorbabackend.models.DTOs.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.security.oauth2.*;
import com.geoffvargo.gvorbabackend.services.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TwoFactorController}. These call the controller directly, so they check how it
 * hands requests to {@link TwoFactorService} and builds responses: the {@code Set-Cookie} header, the
 * body and the status. Request validation, cookie binding and {@code SecurityConfig} rules are not
 * exercised here.
 */
@ExtendWith(MockitoExtension.class)
class TwoFactorControllerTest {

	private static final String USERNAME = "pat@example.com";
	private static final String CODE = "123456";
	private static final String OLD_REFRESH = "old-refresh-token";
	private static final String NEW_REFRESH = "new-refresh-token";

	@Mock private TwoFactorService twoFactor;
	@Mock private RefreshCookieFactory refreshCookies;

	@InjectMocks private TwoFactorController controller;

	private UserDetails userDetails;
	private LoginResponse loginResponse;
	private IssuedSession issuedSession;
	private ResponseCookie newCookie;

	/**
	 * Builds a signed-in principal and a verified session for the service mock to return.
	 */
	@BeforeEach
	void setUp() {
		userDetails = org.springframework.security.core.userdetails.User.withUsername(USERNAME).password("unused").build();
		loginResponse = new LoginResponse("jwt-abc", USERNAME, List.of("ROLE_USER"), false);
		issuedSession = new IssuedSession(loginResponse, NEW_REFRESH);
		newCookie = ResponseCookie.from("refreshToken", NEW_REFRESH).path("/api/auth").httpOnly(true).build();
	}

	/**
	 * Makes the cookie factory return {@link #newCookie} for the new refresh token. Only the
	 * cookie-issuing endpoints call this; with Mockito's strict stubs, an unused stub fails the test.
	 */
	private void stubCookieFactory() {
		when(refreshCookies.buildRefreshCookie(NEW_REFRESH)).thenReturn(newCookie);
	}

	@Nested
	class Status {

		@Test
		void returnsServiceStatusForSignedInUser() {
			StatusResponse expected = new StatusResponse(true);
			when(twoFactor.status(USERNAME)).thenReturn(expected);

			assertThat(controller.status(userDetails)).isSameAs(expected);
		}

		@Test
		void propagatesServiceException() {
			TwoFactorException ex = new TwoFactorException(ErrorCode.MFA_NOT_ENABLED, HttpStatus.NOT_FOUND);
			when(twoFactor.status(USERNAME)).thenThrow(ex);

			assertThatThrownBy(() -> controller.status(userDetails)).isSameAs(ex);
		}
	}

	@Nested
	class Setup {

		@Test
		void returnsUriAndSecretFromService() {
			SetupResponse expected = new SetupResponse("otpauth://totp/gvoRBA:pat?secret=ABC", "ABC");
			when(twoFactor.setup(USERNAME)).thenReturn(expected);

			assertThat(controller.setup(userDetails)).isSameAs(expected);
		}

		@Test
		void alreadyEnrolled_propagatesConflict() {
			TwoFactorException ex = new TwoFactorException(ErrorCode.MFA_ALREADY_ENABLED, HttpStatus.CONFLICT);
			when(twoFactor.setup(USERNAME)).thenThrow(ex);

			assertThatThrownBy(() -> controller.setup(userDetails)).isSameAs(ex);
		}
	}

	@Nested
	class Confirm {

		@Test
		void returnsRecoveryCodesSessionAndNewCookie() {
			List<String> codes = List.of("AAAA-BBBB", "CCCC-DDDD");
			when(twoFactor.confirm(USERNAME, CODE, OLD_REFRESH)).thenReturn(new ConfirmResult(codes, issuedSession));
			stubCookieFactory();

			ResponseEntity<ConfirmResponse> resp = controller.confirm(userDetails, new OtpRequest(CODE), OLD_REFRESH);

			assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isEqualTo(newCookie.toString());
			assertThat(resp.getBody()).isNotNull();
			assertThat(resp.getBody().recoveryCodes()).containsExactlyElementsOf(codes);
			assertThat(resp.getBody().session()).isSameAs(loginResponse);
		}

		@Test
		void missingCookie_passesNullToService() {
			when(twoFactor.confirm(USERNAME, CODE, null)).thenReturn(new ConfirmResult(List.of(), issuedSession));
			stubCookieFactory();

			controller.confirm(userDetails, new OtpRequest(CODE), null);

			verify(twoFactor).confirm(USERNAME, CODE, null);
		}
	}

	@Nested
	class Verify {

		@Test
		void returnsLoginResponseAndNewCookie() {
			when(twoFactor.verify(USERNAME, CODE, OLD_REFRESH)).thenReturn(issuedSession);
			stubCookieFactory();

			ResponseEntity<LoginResponse> resp = controller.verify(userDetails, new OtpRequest(CODE), OLD_REFRESH);

			assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isEqualTo(newCookie.toString());
			assertThat(resp.getBody()).isSameAs(loginResponse);
		}

		@Test
		void wrongCode_propagatesAndIssuesNoCookie() {
			TwoFactorException ex = new TwoFactorException(ErrorCode.INVALID_OTP, HttpStatus.UNAUTHORIZED);
			when(twoFactor.verify(USERNAME, CODE, OLD_REFRESH)).thenThrow(ex);

			assertThatThrownBy(() -> controller.verify(userDetails, new OtpRequest(CODE), OLD_REFRESH)).isSameAs(ex);
		}
	}

	@Nested
	class Recover {

		@Test
		void passesRecoveryCodeAndReturnsNewCookie() {
			String recovery = "AAAA-BBBB";
			when(twoFactor.recover(USERNAME, recovery, OLD_REFRESH)).thenReturn(issuedSession);
			stubCookieFactory();

			ResponseEntity<LoginResponse> resp =
				controller.recover(userDetails, new RecoveryRequest(recovery), OLD_REFRESH);

			assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isEqualTo(newCookie.toString());
			assertThat(resp.getBody()).isSameAs(loginResponse);
		}

		@Test
		void usedRecoveryCode_propagatesAndIssuesNoCookie() {
			TwoFactorException ex =
				new TwoFactorException(ErrorCode.INVALID_RECOVERY_CODE, HttpStatus.UNAUTHORIZED);
			when(twoFactor.recover(eq(USERNAME), anyString(), any())).thenThrow(ex);

			assertThatThrownBy(() -> controller.recover(userDetails, new RecoveryRequest("AAAA-BBBB"), OLD_REFRESH))
				.isSameAs(ex);
			verifyNoInteractions(refreshCookies);
		}
	}

	@Nested
	class Disable {

		@Test
		void returnsNoContentAndSetsNoCookie() {
			ResponseEntity<Void> resp = controller.disable(userDetails, new OtpRequest(CODE));

			assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
			assertThat(resp.getBody()).isNull();
			assertThat(resp.getHeaders().containsHeader(HttpHeaders.SET_COOKIE)).isFalse();
			verify(twoFactor).disable(USERNAME, CODE);
			verifyNoInteractions(refreshCookies);
		}

		@Test
		void wrongCode_propagates() {
			TwoFactorException ex = new TwoFactorException(ErrorCode.INVALID_OTP, HttpStatus.UNAUTHORIZED);
			doThrow(ex).when(twoFactor).disable(USERNAME, CODE);

			assertThatThrownBy(() -> controller.disable(userDetails, new OtpRequest(CODE))).isSameAs(ex);
		}
	}
}
