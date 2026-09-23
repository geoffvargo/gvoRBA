package com.geoffvargo.gvorbabackend.security.oauth2;

import com.geoffvargo.gvorbabackend.exceptions.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.services.*;

import org.jspecify.annotations.NonNull;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.*;
import org.springframework.security.oauth2.client.authentication.*;
import org.springframework.security.web.authentication.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.security.*;
import java.util.*;

import jakarta.servlet.http.*;
import lombok.*;

/**
 * Runs after a successful OAuth2/OIDC login (Google, GitHub, ...).
 * Finds or auto-registers the local {@link User} by email, issues a refresh token
 * as an HTTP cookie, and redirects the browser back to the frontend.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
	public static final Logger LOGGER = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);
	
	private final UserRepository userRepository;
	
	private final RefreshTokenService refreshTokenService;
	
	private final RefreshCookieFactory refreshCookieFactory;
	
	//// Frontend page the browser lands on after login (success or error).
	@Value("${app.oauth2.frontend-redirect-uri}")
	private String frontendRedirectUri;

	@Override
	public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
	                                    @NonNull HttpServletResponse response,
	                                    @NonNull Authentication authentication) throws IOException {
		/// Safe cast: this handler is only wired to oauth2Login(), so it always gets an OAuth2 token.
		OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

		/// Normalize the provider-specific attribute map (e.g. "google", "github") into a common shape.
		OAuth2UserInfo info = OAuth2UserInfoFactory.create(
			oauthToken.getAuthorizedClientRegistrationId(),
			Objects.requireNonNull(oauthToken.getPrincipal()).getAttributes()
		);
		
		/// Email is the key that links an OAuth2 identity to a local account, so it's mandatory.
		String email = info.getEmail();

		if (email == null || email.isBlank()) {
			throw new OAuth2AuthenticationException("Invalid email.");
		}

		/// Existing account -> log in; otherwise create one on the fly (first-time OAuth2 login).
		User user = userRepository.findByEmail(email).orElseGet(
			() -> register(email, info.getName())
		);

		/// Only the refresh token is issued here; the frontend exchanges it for an access token afterwards.
		/// On failure, redirect with an error flag rather than leaving the user on a backend error page.
		String rawRefreshToken;
		try {
			rawRefreshToken = refreshTokenService.issue(user);
		} catch (NoSuchAlgorithmException e) {
			LOGGER.error("Couldn't issue refresh token", e);
			response.sendRedirect(frontendRedirectUri + "?error=server_error");
			return;
		}
		
		/// Deliver the refresh token as a cookie (not in the URL) so it never shows up in history/logs.
		response.addHeader(HttpHeaders.SET_COOKIE,
			refreshCookieFactory.buildRefreshCookie(rawRefreshToken).toString());
		response.sendRedirect(frontendRedirectUri);
	}

	/**
	 * Creates a local account for a first-time OAuth2 user.
	 * No password is set; falls back to the email as display name if the provider didn't supply one.
	 */
	private User register(String email, String name) {
		LOGGER.info("Registering new user from OAuth2 login: {}", email);
		
		User user = User.builder()
			            .email(email)
			            .name(name != null ? name : email)
			            .enabled(true)
			            .build();
		
		return userRepository.save(user);
	}
}
