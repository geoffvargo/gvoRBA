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
	@Column(name = "id", nullable = false)
	private Long id;
	
	@Column(name = "name", nullable = false)
	private String name;
	
	@Column(name = "location")
	private String location;
	
	@Column(name = "capacity")
	private Integer capacity;
	
	@Column(name = "amenities")
	private String[] amenities;
	
	@Column(name = "isActive")
	private Boolean isActive;
	
	@Column(name = "createdOn")
	private Date createdOn;
}
