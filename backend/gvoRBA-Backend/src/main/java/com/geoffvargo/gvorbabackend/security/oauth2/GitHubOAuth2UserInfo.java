package com.geoffvargo.gvorbabackend.security.oauth2;

import java.util.*;

public class GitHubOAuth2UserInfo extends OAuth2UserInfo {
	protected GitHubOAuth2UserInfo(Map<String, Object> attributes) {
		super(attributes);
	}
	
	@Override
	public String getProviderId() {
		return String.valueOf(attributes.get("id"));
	}
	
	@Override
	public String getEmail() {
		return (String) attributes.get("email");
	}
	
	@Override
	public String getName() {
		return (String) attributes.get("name");
	}
}
