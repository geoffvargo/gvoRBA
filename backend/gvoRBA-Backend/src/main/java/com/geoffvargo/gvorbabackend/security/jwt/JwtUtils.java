package com.geoffvargo.gvorbabackend.security.jwt;

import com.geoffvargo.gvorbabackend.security.*;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.*;
import org.springframework.stereotype.*;

import java.security.*;
import java.util.*;
import java.util.stream.*;

import javax.crypto.*;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.*;
import io.jsonwebtoken.security.*;
import jakarta.servlet.http.*;

/**
 * Creates, parses and validates the HMAC-signed JWTs used to authenticate API requests.
 * <p>
 * Tokens carry the username as the subject, the user's roles in a {@code roles} claim and,
 * once a second factor has been verified, the completed factors in a {@value #CLAIMS_FACTORS}
 * claim.
 */
@Component
public class JwtUtils {
	/**
	 * Logger for token parsing and validation events.
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(JwtUtils.class);

	/**
	 * Name of the claim that lists the authentication factors the user has completed.
	 */
	public static final String CLAIMS_FACTORS = "factors";

	/**
	 * How long a newly issued token stays valid, in milliseconds.
	 */
	@Value("${spring.app.jwtExpirationMs}")
	private int expirationMs;

	/**
	 * Base64-encoded HMAC secret used to sign and verify tokens.
	 */
	@Value("${spring.app.jwtSecret}")
	private String secret;

	/**
	 * Returns the username stored as the subject of a signed token.
	 *
	 * @param token
	 * 	the compact, signed JWT
	 *
	 * @return the token's subject
	 *
	 * @throws JwtException
	 * 	if the token is malformed, expired, unsupported or has an invalid signature
	 * @throws IllegalArgumentException
	 * 	if {@code token} is {@code null} or empty
	 */
	public String getUsernameFromToken(String token) {
		return Jwts.parser()
			       .verifyWith((SecretKey) key())
			       .build()
			       .parseSignedClaims(token)
			       .getPayload()
			       .getSubject();
	}

	/**
	 * Builds the HMAC-SHA signing key from the Base64-decoded {@link #secret}.
	 *
	 * @return the key used to sign and verify tokens
	 */
	private Key key() {
		return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
	}

	/**
	 * Extracts the token from the request's {@code Authorization: Bearer} header.
	 *
	 * @param request
	 * 	the incoming HTTP request
	 *
	 * @return the raw token, or {@code null} if the header is missing or not a Bearer header
	 */
	public String getJwtFromHeader(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		LOGGER.debug("Authorization Header: {}", bearerToken);

		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}

		return null;
	}

	/**
	 * Issues a signed token for the user, optionally recording a verified TOTP factor.
	 * <p>
	 * When {@code totpVerified} is {@code true}, the {@value #CLAIMS_FACTORS} claim is set to
	 * {@link Factors#TOTP}; otherwise the claim is omitted.
	 *
	 * @param userDetails
	 * 	the authenticated user to issue the token for
	 * @param totpVerified
	 * 	whether the user has completed TOTP verification
	 *
	 * @return the compact, signed JWT
	 */
	public String generateTokenFromUsername(UserDetailsImpl userDetails, boolean totpVerified) {
		String username = userDetails.getUsername();
		String roles = userDetails.getAuthorities().stream()
			               .map(GrantedAuthority::getAuthority)
			               .collect(Collectors.joining("."));

		Date now = new Date();

		JwtBuilder builder = Jwts.builder()
			                     .subject(username)
			                     .claim("roles", roles)
			                     .issuedAt(now)
			                     .expiration(new Date(now.getTime() + expirationMs));

		if (totpVerified) {
			builder.claim(CLAIMS_FACTORS, List.of(Factors.TOTP));
		}

		return builder.signWith(key())
			       .compact();
	}

	/**
	 * Issues a signed token for the user with no completed-factor claim.
	 *
	 * @param userDetails
	 * 	the authenticated user to issue the token for
	 *
	 * @return the compact, signed JWT
	 */
	public String generateTokenFromUsername(UserDetailsImpl userDetails) {
		String username = userDetails.getUsername();
		String roles = userDetails.getAuthorities().stream()
			               .map(GrantedAuthority::getAuthority)
			               .collect(Collectors.joining("."));

		Date now = new Date();

		return Jwts.builder()
			       .subject(username)
			       .claim("roles", roles)
			       .issuedAt(now)
			       .expiration(new Date(now.getTime() + expirationMs))
			       .signWith(key())
			       .compact();
	}

	/**
	 * Checks whether a token parses and passes signature and expiry verification.
	 * <p>
	 * Malformed, expired, unsupported and empty tokens are logged and reported as invalid.
	 *
	 * @param token
	 * 	the compact, signed JWT
	 *
	 * @return {@code true} if the token is valid, otherwise {@code false}
	 *
	 * @throws io.jsonwebtoken.security.SecurityException
	 * 	if the token's signature does not match, since that case is not caught here
	 */
	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.verifyWith((SecretKey) key())
				.build()
				.parseSignedClaims(token);

			return true;
		} catch (MalformedJwtException e) {
			LOGGER.error("Invalid JWT token: {}", e.getMessage());
		} catch (ExpiredJwtException e) {
			LOGGER.error("Expired JWT token: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			LOGGER.error("Unsupported JWT token: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			LOGGER.error("JWT claims string is empty: {}", e.getMessage());
		}

		return false;
	}

	/**
	 * Verifies a token and returns its claims, or {@code null} if it is rejected.
	 *
	 * @param token
	 * 	the compact, signed JWT
	 *
	 * @return the token's claims, or {@code null} if the token is invalid for any reason
	 */
	public Claims parseValidClaims(String token) {
		try {
			return Jwts.parser()
				       .verifyWith((SecretKey) key())
				       .build()
				       .parseSignedClaims(token)
				       .getPayload();
		} catch (JwtException | IllegalArgumentException e) {
			LOGGER.error("Reqected JWT: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * Converts the {@value #CLAIMS_FACTORS} claim into granted authorities.
	 *
	 * @param claims
	 * 	the verified claims of a token
	 *
	 * @return one authority per completed factor, or an empty list if the claim is absent or
	 * 	not a list
	 */
	public List<GrantedAuthority> factorAuthorization(Claims claims) {
		if (!(claims.get(CLAIMS_FACTORS) instanceof List<?> factors)) {
			return List.of();
		}

		return factors.stream()
			       .map(String::valueOf)
			       .<GrantedAuthority>map(SimpleGrantedAuthority::new)
			       .toList();
	}
}
