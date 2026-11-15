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

	/**
	 * MFA is required only when the user has finished authenticator setup <em>and</em> has the pref on.
	 *
	 * @param enrolled    whether the user has finished authenticator setup
	 * @param pref        whether the user has MFA turned on in their prefs
	 * @param expected    the {@code multifactor} flag the principal should carry
	 */
	@ParameterizedTest(name = "enrolled={0}, pref={1} -> required={2}")
	@CsvSource({
		"true,  true,  true",
		"true,  false, false",
		"false, true,  false",
		"false, false, false",
	})
	void build_requiresMfaOnlyWhenEnrolledAndPrefOn(boolean enrolled, boolean pref, boolean expected) {
		User user = buildUser(AppRole.ROLE_USER, pref);
		user.setTotpEnrolled(enrolled);

		UserDetailsImpl details = UserDetailsImpl.build(user);

		assertEquals(expected, details.isMultifactor());
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