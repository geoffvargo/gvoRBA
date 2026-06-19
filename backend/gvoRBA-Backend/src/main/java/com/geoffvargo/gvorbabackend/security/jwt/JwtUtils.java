package com.geoffvargo.gvorbabackend.security.jwt;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.core.*;
import org.springframework.stereotype.*;

import java.security.*;
import java.util.*;
import java.util.stream.*;

import javax.crypto.*;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.*;
import io.jsonwebtoken.security.*;
import jakarta.servlet.http.*;

@Component
public class JwtUtils {
	public static final Logger LOGGER = LoggerFactory.getLogger(JwtUtils.class);
	
	@Value("${spring.app.jwtExpirationMs}")
	private int expirationMs;
	
	@Value("${spring.app.jwtSecret}")
	private String secret;
	
	public String getUsernameFromToken(String token) {
		return Jwts.parser()
			       .verifyWith((SecretKey) key())
			       .build()
			       .parseSignedClaims(token)
			       .getPayload()
			       .getSubject();
	}
	
	private Key key() {
		return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
	}
	
	public String getJwtFromHeader(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		LOGGER.debug("Authorization Header: {}", bearerToken);
		
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		
		return null;
	}
	
	public String generateTokenFromUsername(UserDetailsImpl userDetails) {
		String username = userDetails.getUsername();
		String roles = userDetails.getAuthorities().stream()
			               .map(GrantedAuthority::getAuthority)
			               .collect(Collectors.joining("."));
		
		return Jwts.builder()
			       .subject(username)
			       .claim("roles", roles)
			       .issuedAt(new Date((new Date()).getTime() + expirationMs))
			       .signWith(key())
			       .compact();
	}
	
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
}
