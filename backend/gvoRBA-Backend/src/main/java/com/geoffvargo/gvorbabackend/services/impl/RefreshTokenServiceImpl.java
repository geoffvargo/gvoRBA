package com.geoffvargo.gvorbabackend.services.impl;

import com.geoffvargo.gvorbabackend.exceptions.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.security.jwt.*;
import com.geoffvargo.gvorbabackend.services.*;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.util.*;

import lombok.*;

@Service
@RequiredArgsConstructor
class RefreshTokenServiceImpl implements RefreshTokenService {
	public static final Logger LOGGER = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);
	
	private final RefreshTokenRepository refreshTokenRepository;
	
	@Value("${app.refresh.ttl-days}")
	private int ttlDays;
	
	@Override
	public String issue(User user) throws NoSuchAlgorithmException {
		byte[] bytes = new byte[32];
		
		SecureRandom sr = new SecureRandom();
		sr.nextBytes(bytes);
		
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		
		refreshTokenRepository.save(RefreshToken.builder()
			                            .user(user)
			                            .tokenHash(hash(raw))
			                            .expiresAt(LocalDateTime.now().plusDays(ttlDays))
			                            .build());
		
		return raw;
	}
	
	private String hash(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (Exception e) {
			throw new IllegalStateException("SHA-256 is required on every Java platform", e);
		}
	}
	
	@Override
	@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
	public RotationResult rotate(String rawToken) {
		Optional<RefreshToken> optional = refreshTokenRepository.findByTokenHash(hash(rawToken));
		
		if (optional.isEmpty()) {
			throw new InvalidRefreshTokenException("No token found!");
		}
		
		RefreshToken row = optional.get();
		
		if (row.getRevokedAt() != null) {
			refreshTokenRepository.revokeAllForUser(LocalDateTime.now(), row.getUser().getId());
			LOGGER.warn("Refresh token replay detected for user id {}", row.getUser().getId());
			throw new InvalidRefreshTokenException("Token already revoked!");
		}
		
		if (row.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new InvalidRefreshTokenException("Token has already expired!");
		}
		
		try {
			UserDetailsImpl principal = UserDetailsImpl.build(row.getUser());
			String newRaw = issue(row.getUser());
			row.setRevokedAt(LocalDateTime.now());
			
			return new RotationResult(newRaw, principal);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is required on every Java platform", e);
		}
	}
	
	@Override
	@Transactional
	public void revoke(String rawToken) {
		Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(hash(rawToken));
		
		found.ifPresent(refreshToken -> refreshToken.setRevokedAt(LocalDateTime.now()));
	}
	
	@Override
	@Scheduled(fixedDelay = 3_600_000)
	@Transactional
	public void purgeExpired() {
		refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
	}
}
