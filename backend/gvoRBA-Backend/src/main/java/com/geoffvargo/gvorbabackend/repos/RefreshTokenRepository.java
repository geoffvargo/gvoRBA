package com.geoffvargo.gvorbabackend.repos;

import com.geoffvargo.gvorbabackend.models.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.*;

import java.time.*;
import java.util.*;

import jakarta.transaction.*;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	Optional<RefreshToken> findByTokenHash(String tokenHash);
	long deleteByExpiresAtBefore(LocalDateTime expiresAt);
	
	@Modifying
	@Query("""
	       UPDATE RefreshToken r
	       SET r.revokedAt = :now
	       WHERE r.user.id = :userId
	         AND r.revokedAt IS NULL
	       """)
	@Transactional
	Integer revokeAllForUser(@Param("now") LocalDateTime now,
	                         @Param("userId") Long userId);
}
