package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;

import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

import lombok.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "http://localhost:4200")
public class RoomController {
	public static final Logger LOGGER = LoggerFactory.getLogger(RoomController.class);
	
	private final RoomRepository roomRepository;
	
	private final BookingRepository bookingRepository;
	
	@GetMapping()
	public ResponseEntity<?> getAllRooms(@RequestParam String name, @RequestParam Integer minCapacity) {
		List<Room> rooms = roomRepository.findAll();
		
		if (name != null) {
			rooms = rooms.stream()
				.filter(room -> room.getName().equals(name))
				.toList();
		}
		if (minCapacity != null) {
			rooms = rooms.stream()
				.filter(r -> r.getCapacity() >= minCapacity)
				.toList();
		}
		
		return ResponseEntity.ok(rooms);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<?> getRoomById(@PathVariable Long id) {
		Room room = roomRepository.findById(id).orElseThrow(
			() -> new UsernameNotFoundException("Room with id " + id + " not found")
		);
		
		return ResponseEntity.ok(room);
	}
	
	@PostMapping("/add-room")
	public ResponseEntity<?> addRoom(@RequestBody AddRoomRequest request) {
		if (roomRepository.existsByName(request.getName())) {
			throw new UsernameNotFoundException("Room with name " + request.getName() + " already exists");
		}
		
		if (roomRepository.existsByLocation(request.getLocation())) {
			throw new UsernameNotFoundException("Room with location " + request.getLocation() + " already exists");
		}
		
		Room room = Room.builder()
			            .name(request.getName())
			            .location(request.getLocation())
			            .capacity(request.getCapacity())
			            .amenities(request.getAmenities())
			            .isActive(request.getIsActive())
			            .createdOn(LocalDateTime.now())
			            .build();
		
		return ResponseEntity.ok(room);
	}
	
	@PostMapping("/add-rooms")
	public ResponseEntity<?> addRooms(@RequestBody List<AddRoomRequest> requests) {
		List<Room> rooms = new ArrayList<>();
		for (AddRoomRequest request : requests) {
			if (roomRepository.existsByName(request.getName())) {
//				throw new UsernameNotFoundException("Room with name " + request.getName() + " already exists");
				LOGGER.error("Room with name {} already exists", request.getName());
				continue;
			}
			
			if (roomRepository.existsByLocation(request.getLocation())) {
//				throw new UsernameNotFoundException("Room with location " + request.getLocation() + " already exists");
				LOGGER.error("Room with location {} already exists", request.getLocation());
				continue;
			}
			
			Room room = Room.builder()
				            .name(request.getName())
				            .location(request.getLocation())
				            .capacity(request.getCapacity())
				            .amenities(request.getAmenities())
				            .isActive(request.getIsActive())
				            .createdOn(LocalDateTime.now())
				            .build();
			rooms.add(room);
			roomRepository.save(room);
		}
		return ResponseEntity.ok(rooms);
	}
	
	@GetMapping("/{id}/bookings")
	public ResponseEntity<?> getBookings(@PathVariable Long id, @RequestParam LocalDateTime date) {
		List<Booking> bookings;
		try {
			bookings = bookingRepository.findByRoom_Id(id).stream()
				           .filter(b -> b.getStartsAt().isEqual(date))
				           .toList();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return ResponseEntity.ok(bookings);
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<?> updateRoom(@PathVariable Long id, @RequestBody AddRoomRequest request) {
		Room room = roomRepository.findById(id).orElseThrow(
			() -> new UsernameNotFoundException("Room with id " + id + " not found")
		);
		
		room.update(request);
		
		roomRepository.save(room);
		
		return ResponseEntity.ok(room);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deactivateRoom(@PathVariable Long id) {
		Room room = roomRepository.findById(id).orElseThrow(
			() -> new UsernameNotFoundException("Room with id " + id + " not found")
		);
		
		room.setIsActive(false);
		
		roomRepository.save(room);
		
		return ResponseEntity.ok(room);
	}
}
