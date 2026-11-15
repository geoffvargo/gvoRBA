package com.geoffvargo.gvorbabackend.security;

import com.geoffvargo.gvorbabackend.exceptions.*;

import org.jspecify.annotations.NonNull;
import org.springframework.http.*;
import org.springframework.security.access.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.web.access.*;
import org.springframework.stereotype.*;

import java.io.*;

import jakarta.servlet.http.*;
import lombok.*;
import lombok.extern.slf4j.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class MfaAccessDeniedHandler implements AccessDeniedHandler {
	private final ApiErrorWriter errors;
	
	@Override
	public void handle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
	                   @NonNull AccessDeniedException accessDeniedException) throws IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		
		if (TotpFactorAuthorizationManager.requiresTotp(auth) && !TotpFactorAuthorizationManager.hasTotp(auth)) {
			errors.write(
				request,
				response,
				HttpStatus.UNAUTHORIZED,
				ErrorCode.MFA_REQUIRED,
				"Two-factor verification required."
			);
			
			return;
		}
		errors.write(
			request,
			response,
			HttpStatus.FORBIDDEN,
			ErrorCode.FORBIDDEN,
			"Access Denied."
		);
	}
}
