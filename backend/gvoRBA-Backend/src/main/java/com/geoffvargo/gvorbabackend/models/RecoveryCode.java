package com.geoffvargo.gvorbabackend.models;

import java.time.*;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @ToString @NoArgsConstructor
@Table(name = "recovery_code")
public class RecoveryCode {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(nullable = false)
	private Long id;
	
	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private User user;
	
	@Column(name = "code_hash", nullable = false, length = 64)
	private String codeHash;
	
	@Column(name = "used_at")
	private LocalDateTime usedAt;
}
