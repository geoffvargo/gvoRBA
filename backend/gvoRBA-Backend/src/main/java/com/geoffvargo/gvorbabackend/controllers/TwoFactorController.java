package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.exceptions.*;
import com.geoffvargo.gvorbabackend.models.DTOs.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.security.oauth2.*;
import com.geoffvargo.gvorbabackend.services.*;

import org.springframework.http.*;
import org.springframework.security.core.annotation.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.*;
import lombok.*;
import lombok.extern.slf4j.*;

/**
 * /api/auth/2fa/**. SecurityConfig lets a pending (not yet verified) session call only verify and recover;
 * status, setup, confirm and disable need a verified session (or any session when 2FA is off).
 * <p>
 * The refresh cookie is path-scoped to /api/auth, so the browser sends it to every endpoint here.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/2fa")
public class TwoFactorController {
	private static final String REFRESH_COOOKIE = "refreshToken";
	
	private final TwoFactorService twoFactor;
	
	private final RefreshCookieFactory refreshCookies;
	
	/**
	 * Starts a 200 OK response that carries the new, verified refresh token as a cookie. The browser
	 * replaces its old {@code refreshToken} cookie with it.
	 *
	 * @param session
	 * 	the session {@link TwoFactorService} just issued
	 * @return the response builder, without a body: each caller adds its own
	 */
	private ResponseEntity.BodyBuilder withRefreshCookie(IssuedSession session) {
		return ResponseEntity.ok()
			       .header(HttpHeaders.SET_COOKIE,
				       String.valueOf(
					       refreshCookies.buildRefreshCookie(
						       session.rawRefreshToken())));
	}
	
	/**
	 * {@code GET /api/auth/2fa/status}: reports whether the signed-in user has finished authenticator setup.
	 *
	 * @param userDetails
	 * 	the signed-in user's corresponding {@code UserDetails} object, injected by Spring Security
	 * @return the user's enrollment status, as JSON
	 * @throws TwoFactorException
	 * 	if the user no longer exists
	 */
	@GetMapping("/status")
	public StatusResponse status(@AuthenticationPrincipal UserDetails userDetails) {
		return twoFactor.status(userDetails.getUsername());
	}
	
	/**
	 * {@code POST /api/auth/2fa/setup}: enrollment part 1. Creates a new TOTP secret for the signed-in user.
	 *
	 * @param userDetails
	 * 	the signed-in user's coreesponding {@code UserDetals} obeject, injected by Spring Security
	 * @return the {@code otpauth://} URI to render as a QR code, and the secret in Base32 for manual entry
	 * @throws TwoFactorException
	 * 	with {@code MFA_ALREADY_ENABLED} (409) if the user is already enrolled
	 */
	@PostMapping("/setup")
	public SetupResponse setup(@AuthenticationPrincipal UserDetails userDetails) {
		return twoFactor.setup(userDetails.getUsername());
	}
	
	/**
	 * {@code POST /api/auth/2fa/confirm}: enrollment part 2. Checks the first code from the authenticator app,
	 * marks the user enrolled, and returns their recovery codes and a verified session.
	 *
	 * @param userDetails
	 * 	the signed-in user's corresponding {$code UserDetails} object, injected by Spring Security
	 * @param request
	 * 	the request JSON, holding the 6-digit code
	 * @param refreshToken
	 * 	the browser's {@code refreshToken} cookie, or {@code null} if it sent none
	 * @return 200 with a {@code Set-Cookie} header for the new verified refresh token, and a body holding
	 * 	the recovery codes (shown once) and the new {@link LoginResponse}
	 * @throws TwoFactorException
	 * 	if too many attempts failed, the user is already enrolled, setup wasn't
	 * 	started, or the code is wrong or already used
	 */
	@PostMapping("/confirm")
	public ResponseEntity<ConfirmResponse> confirm(@AuthenticationPrincipal UserDetails userDetails,
	                                               @Valid @RequestBody OtpRequest request,
	                                               @CookieValue(name = REFRESH_COOOKIE,
	                                                            required = false) String refreshToken) {
		ConfirmResult res = twoFactor.confirm(userDetails.getUsername(), request.code(), refreshToken);
		LoginResponse response = res.session().body();
		ResponseEntity.BodyBuilder ans = withRefreshCookie(res.session());
		
		return ans.body(new ConfirmResponse(res.recoveryCodes(), response));
	}
	
	/**
	 * {@code POST /api/auth/2fa/verify}: upgrades a pending session to a verified one with a valid code.
	 *
	 * @param userDetails
	 * 	the signed-in user' {@code UserDetails} object, injected by Spring Security
	 * @param request
	 * 	the request JSON, holding the 6-digit code
	 * @param refreshToken
	 * 	the browser's {@code refreshToken} cookie, or {@code null} if it sent none
	 * @return 200 with a {@code Set-Cookie} header for the new verified refresh token, and the new
	 *   {@link LoginResponse} as the body
	 * @throws TwoFactorException
	 * 	if too many attempts failed, the user isn't enrolled, or the code is
	 * 	wrong or already used
	 */
	@PostMapping("/verify")
	public ResponseEntity<LoginResponse> verify(@AuthenticationPrincipal UserDetails userDetails,
	                                            @Valid @RequestBody OtpRequest request,
	                                            @CookieValue(name = REFRESH_COOOKIE,
	                                                         required = false) String refreshToken) {
		IssuedSession session = twoFactor.verify(userDetails.getUsername(), request.code(), refreshToken);
		LoginResponse resp = session.body();
		val ans = withRefreshCookie(session);
		
		return ans.body(resp);
	}
	
	/**
	 * {@code POST /api/auth/2fa/recover}: upgrades a pending session to a verified one with an unused
	 * recovery code, which is used up.
	 *
	 * @param userDetails
	 * 	the signed-in user' corresponding {@code UserDetails} object, injected by Spring Security
	 * @param request
	 * 	the request JSON, holding the recovery code as the user typed it
	 * @param refreshToken
	 * 	the browser's {@code refreshToken} cookie, or {@code null} if it sent none
	 * @return 200 with a {@code Set-Cookie} header for the new verified refresh token, and the new
	 *   {@link LoginResponse} as the body
	 * @throws TwoFactorException
	 * 	if too many attempts failed, the user isn't enrolled, or the recovery
	 * 	code is wrong or already used
	 */
	@PostMapping("/recover")
	public ResponseEntity<LoginResponse> recover(@AuthenticationPrincipal UserDetails userDetails,
	                                             @Valid @RequestBody RecoveryRequest request,
	                                             @CookieValue(name = REFRESH_COOOKIE,
	                                                          required = false) String refreshToken) {
		IssuedSession session = twoFactor.recover(userDetails.getUsername(), request.recoveryCode(), refreshToken);
		LoginResponse resp = session.body();
		val ans = withRefreshCookie(session);
		
		return ans.body(resp);
	}
	
	/**
	 * {@code POST /api/auth/2fa/disable}: removes the signed-in user's authenticator enrollment. Needs a
	 * current code, so a stolen session alone can't do it. Doesn't change the user's {@code multifactor}
	 * preference.
	 *
	 * @param userDetails the signed-in user's {@code de UserDetails} object, injected by Spring Security
	 * @param request the request JSON, holding a current 6-digit code
	 * @return 204 No Content, with no body
	 * @throws TwoFactorException if too many attempts failed, the user isn't enrolled, or the code is
	 *                            wrong or already used
	 */
	@PostMapping("/disable")
	public ResponseEntity<Void> disable(@AuthenticationPrincipal UserDetails userDetails,
	                                    @Valid @RequestBody OtpRequest request) {
		twoFactor.disable(userDetails.getUsername(), request.code());
		
		return ResponseEntity.noContent().build();
	}
}
