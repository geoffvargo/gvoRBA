package com.geoffvargo.gvorbabackend.models;

import com.fasterxml.jackson.annotation.*;

import java.util.*;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;
	
	@Column(name = "email")
	private String email;
	
	@Column(name = "password")
	private String password;
	
	@Column(name = "name")
	private String name;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role", referencedColumnName = "role_id")
	@JsonIgnoreProperties
	@ToString.Exclude
	private Role role;
	
	@Column(name = "created_on")
	private Date createdOn;
	
	@Column(name = "enabled", nullable = false)
	private Boolean enabled;
	
	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(name = "auth_provider")
	private AuthProvider authProvider = AuthProvider.LOCAL;
	
	@Column(name = "provider_id")
	private String providerId;
	
	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private UserPrefs userPrefs;
	
	/** AES-GCM ciphertext (Base64) of the raw 20-byte TOTP secret. Null until setup. */
	@Column(name = "totp_secret")
	private String totpSecret;
	
	/** Highest TOTP time step ever accepted. Written only by UserRepository.claimTotpStep. */
	@Column(name = "totp_last_step")
	private Long totpLastStep;
	
	/**
	 * Attaches a default {@link UserPrefs} if this user has none, so it gets saved along with the user.
	 *
	 * @return this user
	 */
	public User withDefaultPrefs() {
		if (userPrefs == null) {
			userPrefs = UserPrefs.builder().user(this).build();
		}

		return this;
	}
}
