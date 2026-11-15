package com.geoffvargo.gvorbabackend.security;

import com.geoffvargo.gvorbabackend.exceptions.*;

import org.springframework.http.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.time.*;

import jakarta.servlet.http.*;
import lombok.*;
import tools.jackson.databind.json.*;

/**
 * Writes an ApiError from inside the security filter chain, where @RestControllerAdvice cannot reach.
 * Uses Spring Boot 4's auto-configured Jackson 3 mapper, so Instant and @JsonFormat behave exactly as
 * they do in controller responses.
 */
@Component
@RequiredArgsConstructor
public class ApiErrorWriter {
	private final JsonMapper jsonMapper;
	
	public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
	                  ErrorCode errorCode, String str) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		
		ApiError body = new ApiError(
			Instant.now(),
			status.value(),
			errorCode,
			str,
			request.getRequestURI()
		);
		
		jsonMapper.writeValue(response.getOutputStream(), body);
	}
}
