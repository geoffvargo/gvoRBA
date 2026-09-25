package com.geoffvargo.gvorbabackend.security.oauth2;

import com.geoffvargo.gvorbabackend.exceptions.*;
import com.geoffvargo.gvorbabackend.models.*;

import java.util.*;

public class OAuth2UserInfoFactory {
	public static OAuth2UserInfo create(String registrationId, Map<String, Object> attributes) {
		return switch (AuthProvider.fromRegistrationId(registrationId)) {
			case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
			case GITHUB -> new GitHubOAuth2UserInfo(attributes);
			default -> throw new OAuth2AuthException("registrationId is invalid");
		};
	}
}
