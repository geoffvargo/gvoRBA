package com.geoffvargo.gvorbabackend.models.DTOs;

/** Response body for GET /api/auth/2fa/status. */
public record StatusResponse(boolean enabled) {
}
