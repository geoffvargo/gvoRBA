package com.geoffvargo.gvorbabackend.security.oauth2;

import java.util.*;

public abstract class OAuth2UserInfo {
	protected final Map<String, Object> attributes;
	protected OAuth2UserInfo(Map<String, Object> attributes) {
		this.attributes = attributes;
	}
	
	public Boolean isEmailVerified() {
		return Boolean.TRUE.equals(attributes.get("email_verified"));
	}
	
	public abstract String getProviderId();
	public abstract String getEmail();
	public abstract String getName();
}
