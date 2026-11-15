package com.geoffvargo.gvorbabackend.models.DTOs;

import jakarta.validation.constraints.*;

/** Request body for /api/auth/2fa/recover: one recovery code, with or without its dashes. */
public record RecoveryRequest(@NotBlank String recoveryCode) {
}
