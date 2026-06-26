package com.geoffvargo.gvorbabackend.models;

import java.time.*;
import java.util.*;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rooms")
public class Room {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;
	
	@Column(name = "name", nullable = false)
	private String name;
	
	@Column(name = "location")
	private String location;
	
	@Column(name = "capacity")
	private Integer capacity;
	
	@Column(name = "amenities")
	private List<String> amenities;
	
	@Column(name = "isActive")
	private Boolean isActive;
	
	@Column(name = "createdOn")
	private LocalDateTime createdOn;
	
	public Room(String name, String location, Integer capacity, List<String> amenities, Boolean isActive,
	            LocalDateTime createdOn) {
		this.name = name;
		this.location = location;
		this.capacity = capacity;
		this.amenities = amenities;
		this.isActive = isActive;
		this.createdOn = createdOn;
	}
}
