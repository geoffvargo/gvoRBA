package com.geoffvargo.gvorbabackend.security.oauth2;

import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;

import java.time.*;

@Component
public class RefreshCookieFactory {
	@Value("${app.refresh.ttl-days}")
	int refreshTtlDays;
	
	@Value("${app.cookie.same-site}")
	String cookieSameSite;
	
	public RefreshCookieFactory() {
	}
	
	ResponseCookie buildRefreshCookie(String rawToken) {
		ResponseCookie ans = ResponseCookie.from("refreshToken")
			                     .value(rawToken)
			                     .path("/api/auth")
			                     .httpOnly(true)
			                     .secure(true)
			                     .sameSite(cookieSameSite)
			                     .maxAge(Duration.ofDays(refreshTtlDays))
			                     .build();
		
		OAuth2LoginSuccessHandler.LOGGER.info(ans.toString());
		
		return ans;
	}
}
