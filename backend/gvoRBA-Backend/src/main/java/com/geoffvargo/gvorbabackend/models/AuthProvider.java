package com.geoffvargo.gvorbabackend.models;

import com.geoffvargo.gvorbabackend.exceptions.*;

public enum AuthProvider {
	LOCAL,
	GOOGLE,
	GITHUB;
	
	public static AuthProvider fromRegistrationId(String registrationId) {
		return switch (registrationId.toLowerCase()) {
			case "local" -> AuthProvider.LOCAL;
			case "google" -> AuthProvider.GOOGLE;
			case "github" -> AuthProvider.GITHUB;
			default -> throw new OAuth2AuthException("regidtrationId is invalid!");
		};
	}
}
