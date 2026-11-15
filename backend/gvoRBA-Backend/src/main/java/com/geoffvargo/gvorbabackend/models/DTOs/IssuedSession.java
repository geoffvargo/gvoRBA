package com.geoffvargo.gvorbabackend.models.DTOs;

import com.geoffvargo.gvorbabackend.models.*;

/** Service → controller: the response body, plus the raw refresh token the controller puts in the cookie. */
public record IssuedSession(
	LoginResponse body,
	String rawRefreshToken
) {}
