package com.geoffvargo.gvorbabackend.models;

import java.util.*;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;
	
	@Column(name = "email")
	private String email;
	
	@Column(name="password")
	private String password;
	
	@Column(name="name")
	private String name;
	
	@Column(name="role")
	private Role role;
	
	@Column(name="created_on")
	private Date createdOn;
}
