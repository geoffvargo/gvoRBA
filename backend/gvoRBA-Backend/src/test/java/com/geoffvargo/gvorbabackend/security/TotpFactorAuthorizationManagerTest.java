package com.geoffvargo.gvorbabackend.security;

import com.geoffvargo.gvorbabackend.security.jwt.*;

import org.junit.jupiter.api.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.*;

import java.util.*;

import static org.junit.Assert.*;

class TotpFactorAuthorizationManagerTest {
	private final TotpFactorAuthorizationManager manager = new TotpFactorAuthorizationManager();
	
	@Test
	void userWithoutTwoFactorIsGranted() {
		assertTrue(isGranted(false, List.of()));
	}
	
	@Test
	void enrolledUserWithoutTotpFactorIsDenied() {
		assertFalse(isGranted(true, List.of()));
	}
	
	@Test
	void enrolledUserWithTotpFactorIsGranted() {
		assertTrue(isGranted(true, List.of(new SimpleGrantedAuthority(Factors.TOTP))));
	}
	
	@Test
	void noAuthenticationIsLeftToTheAuthenticatedRule() {
		assertTrue(Objects.requireNonNull(manager.authorize(() -> null, new Object())).isGranted());
	}
	
	private boolean isGranted(boolean multifactor, List<SimpleGrantedAuthority> factorAuthorities) {
		UserDetailsImpl user = new UserDetailsImpl(
			1L,
			"alice",
			"alice@example.com",
			"{noop}x",
			multifactor,
			List.of(new SimpleGrantedAuthority("ROLE_USER")));
//		user.setMultifactor(true);
		
		List<GrantedAuthority> authorities = new ArrayList<>(user.getAuthorities());
		authorities.addAll(factorAuthorities);
		Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, authorities);
		
		return Objects.requireNonNull(manager.authorize(() -> authentication, new Object())).isGranted();
		
	}
}
