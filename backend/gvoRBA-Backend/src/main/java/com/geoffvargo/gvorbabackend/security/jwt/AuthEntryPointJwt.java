package com.geoffvargo.gvorbabackend.security.jwt;

import com.geoffvargo.gvorbabackend.exceptions.*;
import com.geoffvargo.gvorbabackend.security.*;

import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.security.core.*;
import org.springframework.security.web.*;
import org.springframework.stereotype.*;

import java.io.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.*;

@Component
@RequiredArgsConstructor
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthEntryPointJwt.class);
	
	private final ApiErrorWriter errorWriter;
	
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
	                     AuthenticationException authException) throws IOException, ServletException {
		LOGGER.error("Unauthorized access to {} : {}", request.getRequestURI(), authException.getMessage());
		
		errorWriter.write(
			request,
			response,
			HttpStatus.UNAUTHORIZED,
			ErrorCode.AUTHENTICATION_REQUIRED,
			"Authentication required."
		);
	}
}
