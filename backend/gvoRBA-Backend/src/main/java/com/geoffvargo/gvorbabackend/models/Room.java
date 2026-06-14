package com.geoffvargo.gvorbabackend.models;

import java.util.*;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Table(name = "rooms")
public class Room {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(nullable = false)
	private Long id;
	
	@Column(nullable = false)
	private String name;
	
	private String location;
	
	private Integer capacity;
	
	private String[] amenities;
	
	private Boolean isActive;
	
	private Date createdOn;
}
