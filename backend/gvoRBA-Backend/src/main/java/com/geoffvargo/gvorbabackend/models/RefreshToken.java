package com.geoffvargo.gvorbabackend.models;

import java.time.*;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Table(name = "refresh_token")
public class RefreshToken {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(nullable = false)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;
	
	@Column(name = "token_hash", nullable = false, unique = true)
	private String tokenHash;
	
	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;
	
	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;
}
