package com.geoffvargo.gvorbabackend.models;

import com.fasterxml.jackson.annotation.*;

import java.util.*;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class Role {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "role_id", nullable = false)
	private Long id;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "role_name", nullable = false)
	private AppRole roleName;
	
	@OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
	@JsonBackReference
	@ToString.Exclude
	private Set<User> users = new HashSet<>();
	
	public Role(AppRole roleName) {
		this.roleName = roleName;
	}
}
