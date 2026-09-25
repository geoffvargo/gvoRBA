package com.geoffvargo.gvorbabackend.exceptions;

public class OAuth2AuthException extends RuntimeException {
	public OAuth2AuthException(String message) {
		super(message);
	}
}
