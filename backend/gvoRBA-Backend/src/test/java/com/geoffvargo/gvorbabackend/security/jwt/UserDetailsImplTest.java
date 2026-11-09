package com.geoffvargo.gvorbabackend.security.jwt;

import com.geoffvargo.gvorbabackend.models.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.security.core.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UserDetailsImpl#build(User)}, which turns a stored user into a Spring Security principal.
 */
class UserDetailsImplTest {

	/// --- tests ---

	@Test
	void build_copiesIdentityFields() {
		User user = buildUser(AppRole.ROLE_USER, false);

		UserDetailsImpl details = UserDetailsImpl.build(user);

		assertEquals(user.getId(), details.getId());
		assertEquals(user.getName(), details.getUsername());
		assertEquals(user.getEmail(), details.getEmail());
		assertEquals(user.getPassword(), details.getPassword());
	}

	@ParameterizedTest
	@EnumSource(AppRole.class)
	void build_grantsUsersRoleAsOnlyAuthority(AppRole role) {
		UserDetailsImpl details = UserDetailsImpl.build(buildUser(role, false));

		List<String> authorities = details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

		assertEquals(List.of(role.name()), authorities);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void build_copiesMultifactorPref(boolean multifactor) {
		UserDetailsImpl details = UserDetailsImpl.build(buildUser(AppRole.ROLE_USER, multifactor));

		assertEquals(multifactor, details.isMultifactor());
	}

	/// --- helpers ---

	/**
	 * Builds a user with prefs attached, since {@code build()} reads the multifactor pref.
	 *
	 * @param role        the user's role
	 * @param multifactor whether the user has MFA turned on
	 * @return the user
	 */
	private static User buildUser(AppRole role, boolean multifactor) {
		User user = User.builder()
			            .id(1L)
			            .name("Jane Doe")
			            .email("jane@example.com")
			            .password("hashedPassword")
			            .role(new Role(role))
			            .build()
			            .withDefaultPrefs();
		user.getUserPrefs().setMultifactor(multifactor);
		return user;
	}
}