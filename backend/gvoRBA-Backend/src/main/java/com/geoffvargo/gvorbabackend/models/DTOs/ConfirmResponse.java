package com.geoffvargo.gvorbabackend.models.DTOs;

import com.geoffvargo.gvorbabackend.models.*;

import java.util.*;

/** Response body for /api/auth/2fa/confirm. recoveryCodes are shown to the user exactly once; only their hashes are stored. */
public record ConfirmResponse(
	List<String> recoveryCodes,
	LoginResponse session
) {}
