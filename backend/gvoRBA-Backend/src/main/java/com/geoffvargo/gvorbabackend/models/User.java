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
}
