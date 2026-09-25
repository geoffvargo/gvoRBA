package com.geoffvargo.gvorbabackend.security.oauth2;

import org.jspecify.annotations.NonNull;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.*;
import org.springframework.stereotype.*;

import java.io.*;

import jakarta.servlet.http.*;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {
	public static final Logger LOGGER = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);
	
	@Value("${app.oauth2.frontend-redirect-uri}")
	private String frontendRedirectUri;
	
	@Override
	public void onAuthenticationFailure(@NonNull HttpServletRequest request,
	                                    @NonNull HttpServletResponse response,
	                                    @NonNull AuthenticationException exception) throws IOException {
		LOGGER.error("OAuth2 login failed at {}", request.getRequestURI(), exception);

		/// Provider errors carry a standard code (e.g. "invalid_client", "access_denied").
		String code = exception instanceof OAuth2AuthenticationException oauth2Ex
			              ? oauth2Ex.getError().getErrorCode()
			              : "login_failed";

		response.sendRedirect(frontendRedirectUri + "?error=" + URLEncoder.encode(code, StandardCharsets.UTF_8));
	}
}
