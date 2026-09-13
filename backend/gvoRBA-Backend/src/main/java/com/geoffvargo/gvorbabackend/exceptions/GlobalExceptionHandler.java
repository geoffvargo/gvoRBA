package com.geoffvargo.gvorbabackend.exceptions;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.*;

import jakarta.servlet.http.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(OverlapConflictException.class)
	public ResponseEntity<ApiError> handleConflict(OverlapConflictException ex, HttpServletRequest request) {
		ApiError err = new ApiError(
			Instant.now(),
			HttpStatus.CONFLICT.value(),
			ErrorCode.BOOKING_CONFLICT,
			ex.getMessage(),
			request.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
	}
	
	@ExceptionHandler(InvalidRefreshTokenException.class)
	public ResponseEntity<ApiError> handleRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request) {
		ApiError err = new ApiError(
			Instant.now(),
			HttpStatus.UNAUTHORIZED.value(),
			ErrorCode.INVALID_REFRESH_TOKEN,
			ex.getMessage(),
			request.getRequestURI());
		
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
	}
}
