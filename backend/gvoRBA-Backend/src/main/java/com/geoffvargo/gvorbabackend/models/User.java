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
	
	private String email;
	
	private String password;
	
	private String name;
	
	private Role role;
	
	private Date createdOn;
}
