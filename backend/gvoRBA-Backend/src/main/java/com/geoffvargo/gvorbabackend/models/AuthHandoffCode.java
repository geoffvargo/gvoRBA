package com.geoffvargo.gvorbabackend.models;

import java.time.*;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "auth_handoff_code")
// Not wired up yet — no code currently creates, consumes, or reads rows of this entity.
public class AuthHandoffCode {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id")
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;
	
	@Column(name = "code_hash")
	private String codeHash;
	
	@Column(name = "expires_at")
	private LocalDateTime expiresAt;
	
	@Column(name = "consumed_at")
	private LocalDateTime consumedAt;
}
