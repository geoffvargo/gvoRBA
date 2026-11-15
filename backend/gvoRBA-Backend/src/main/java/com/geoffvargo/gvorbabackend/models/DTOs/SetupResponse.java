package com.geoffvargo.gvorbabackend.models.DTOs;

/** Response body for /api/auth/2fa/setup. otpauthUri is rendered as a QR code; secret is shown for manual entry. */
public record SetupResponse(
	String otpAuthUri,
	String secret
) {}
