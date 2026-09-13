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

import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.util.*;

import javax.swing.text.html.*;

import jakarta.transaction.*;
import lombok.*;

@Service
@RequiredArgsConstructor
class RefreshTokenServiceImpl implements RefreshTokenService {
	public static final Logger LOGGER = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);
	
	private final RefreshTokenRepository refreshTokenRepository;
	
	@Value("${spring.app.jwtExpirationMs}")
	private int expirationMs;
	
	@Override
	public String issue(User user) throws NoSuchAlgorithmException {
		byte[] bytes = new byte[32];
		
		SecureRandom sr = new SecureRandom();
		sr.nextBytes(bytes);
		
		String raw = Base64.getEncoder().encodeToString(bytes);
		
		byte[] digest = MessageDigest.getInstance("SHA-256")
			                .digest(raw.getBytes(StandardCharsets.UTF_8));
		String ans = HexFormat.of().formatHex(digest);
		LOGGER.info(ans);
		LocalDateTime expDate = LocalDateTime.now().plusSeconds(expirationMs / 60);
		
		refreshTokenRepository.save(RefreshToken.builder()
			                            .user(user)
			                            .tokenHash(ans)
			                            .expiresAt(expDate)
			                            .build());
		
		return ans;
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
	@Transactional
	public RotationResult rotate(String rawToken) {
		Optional<RefreshToken> optional = refreshTokenRepository.findByTokenHash(hash(rawToken));
		
		if (optional.isEmpty()) {
			throw new InvalidRefreshTokenException("No token found!");
		}
		
		RefreshToken row = optional.get();
		
		if (row.getRevokedAt() != null) {
			refreshTokenRepository.revokeAllForUser(LocalDateTime.now(), row.getUser().getId());
			throw new InvalidRefreshTokenException("Token already revoked!");
		}
		
		if (row.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new InvalidRefreshTokenException("Token has already expired!");
		}
		
		try {
			UserDetailsImpl principal = UserDetailsImpl.build(row.getUser());
			String newRaw = issue(row.getUser());
			row.setRevokedAt(LocalDateTime.now());
			
			RotationResult rotationResult = new RotationResult(newRaw, principal);
			LOGGER.info(rotationResult.toString());
			
			return rotationResult;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
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
	public void purgeExpired() {
		refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
	}
}
