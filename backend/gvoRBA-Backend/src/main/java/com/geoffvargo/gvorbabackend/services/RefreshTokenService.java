package com.geoffvargo.gvorbabackend.services;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.security.jwt.*;

import java.security.*;

public interface RefreshTokenService {
	record RotationResult(String rawToken, UserDetailsImpl principal) {
	}
	
	public String issue(User user) throws NoSuchAlgorithmException;
	public RotationResult rotate(String rawToken);
	void revoke(String rawToken);
	void purgeExpired();
}
