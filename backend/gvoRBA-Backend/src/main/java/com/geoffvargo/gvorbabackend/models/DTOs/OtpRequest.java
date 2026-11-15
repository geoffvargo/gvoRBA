package com.geoffvargo.gvorbabackend.models.DTOs;

import jakarta.validation.constraints.*;

/** Request body for /api/auth/2fa/confirm, /verify and /disable: one 6-digit authenticator code. */
public record OtpRequest(
	@NotBlank
	@Pattern(regexp = "\\d{6}", message = "must be 6 digits")
	String code
) {}
