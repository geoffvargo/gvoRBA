package com.geoffvargo.gvorbabackend.exceptions;

public class OAuth2AuthenticationException extends RuntimeException {
	public OAuth2AuthenticationException(String message) {
		super(message);
	}
}
