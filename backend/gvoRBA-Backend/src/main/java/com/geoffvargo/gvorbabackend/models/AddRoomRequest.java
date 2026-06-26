package com.geoffvargo.gvorbabackend.models;

import java.util.*;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddRoomRequest {
	private String name;
	private String location;
	private Integer capacity;
	private List<String> amenities;
	private Boolean isActive;
}
