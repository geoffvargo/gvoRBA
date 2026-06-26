package com.geoffvargo.gvorbabackend.models.DTOs;

import com.geoffvargo.gvorbabackend.models.*;

import java.io.*;
import java.time.*;
import java.util.*;

import lombok.*;

/**
 * DTO for {@link com.geoffvargo.gvorbabackend.models.Room}
 */
@Builder
public record RoomDto(Long id, String name, String location, Integer capacity, List<String> amenities,
                      Boolean isActive, LocalDateTime createdOn) implements Serializable {
	public static final RoomDto fromRoom(Room room) {
		return new RoomDto(
			room.getId(),
			room.getName(),
			room.getLocation(),
			room.getCapacity(),
			room.getAmenities(),
			room.getIsActive(),
			room.getCreatedOn()
		);
	}
	
	public static final RoomDto toNewDto(String name, String location, Integer capacity, List<String> amenities,
	                                    Boolean isActive) {
		return RoomDto.builder()
			       .name(name)
			       .location(location)
			       .capacity(capacity)
			       .amenities(amenities)
			       .isActive(isActive)
			       .createdOn(LocalDateTime.now())
			       .build();
	}
}
