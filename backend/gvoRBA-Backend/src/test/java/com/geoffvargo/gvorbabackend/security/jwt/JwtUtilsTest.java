package com.geoffvargo.gvorbabackend.security.jwt;

import com.geoffvargo.gvorbabackend.models.*;

import org.junit.jupiter.api.*;
import org.slf4j.*;
import org.springframework.test.util.*;

import java.util.Base64;

import javax.crypto.*;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.*;
import io.jsonwebtoken.security.*;

import static org.junit.Assert.*;

class JwtUtilsTest {
	// Must be >= 256 bits (32 bytes) for HS256.
	private static final String TEST_SECRET =
		Base64.getEncoder().encodeToString("test-secret-key-that-is-32-bytes!!".getBytes());
	private static final int EXPIRATION_MS = 3_600_000; // 1 hour
	public static final Logger LOGGER = LoggerFactory.getLogger(JwtUtilsTest.class);
	private JwtUtils jwtUtils;
	
	@BeforeEach
	void setUp() {
		jwtUtils = new JwtUtils();
		ReflectionTestUtils.setField(jwtUtils, "secret", TEST_SECRET);
		ReflectionTestUtils.setField(jwtUtils, "expirationMs", EXPIRATION_MS);
	}
	
	// --- helpers ---
	
	@Test
	void generateTokenFromUsername_subjectIsUsername() {
		UserDetailsImpl details = buildDetails(userWithRole(AppRole.ROLE_USER));
		
		String token = jwtUtils.generateTokenFromUsername(details);
		
		LOGGER.info(token);
		
		assertEquals(AppRole.ROLE_USER.name(), parse(token).get("roles", String.class));
	}
	
	private UserDetailsImpl buildDetails(User user) {
		return UserDetailsImpl.build(user);
	}
	
	private User userWithRole(AppRole appRole) {
		return User.builder()
			       .id(1L)
			       .name("jane")
			       .email("jane@example.com")
			       .password("hashed")
			       .role(new Role(appRole))
			       .build();
	}
	
	// --- tests ---
	
	private Claims parse(String token) {
		SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
		return Jwts.parser()
			       .verifyWith(key)
			       .build()
			       .parseSignedClaims(token)
			       .getPayload();
	}
}
