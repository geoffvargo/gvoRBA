package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.models.User;
import com.geoffvargo.gvorbabackend.repos.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.security.core.*;
import org.springframework.security.core.annotation.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.logging.*;

import lombok.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "http://localhost:4200")
public class BookingController {
	public static final Logger LOGGER = Logger.getLogger(BookingController.class.getName());
	
	private final BookingRepository bookingRepository;
	
	private final UserRepository userRepository;
	
	private final RoomRepository roomRepository;
	
	@GetMapping()
	public ResponseEntity<List<Booking>> getAllBookings() {
		return ResponseEntity.ok(bookingRepository.findAll());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<Booking> getBookingById(@PathVariable Long id) {
		Booking booking = bookingRepository.findById(id).orElseThrow(
			() -> new BookingNotFoundException("Booking with id {} not found.", id)
		);
		
		return ResponseEntity.ok(booking);
	}
	
	@PostMapping("/add-booking")
	public ResponseEntity<Booking> addBooking(@RequestBody BookingRequest request) {
		// TODO: make sure this returns 409 error on booking overlap error
		User user = userRepository.findById(request.getUserId()).orElseThrow();
		
		Room room = roomRepository.findById(request.getRoomId()).orElseThrow();
		
		Booking booking = Booking.builder()
			                  .room(room)
			                  .userId(user)
			                  .startsAt(request.getStartsAt())
			                  .endsAt(request.getEndsAt())
			                  .purpose(request.getPurpose())
			                  .status(request.getStatus())
			                  .build();
		
		bookingRepository.save(booking);
		return ResponseEntity.ok(booking);
	}
	
	@PostMapping("/add-bookings")
	public ResponseEntity<List<Booking>> addBookings(@RequestBody List<BookingRequest> requests) {
		User user;
		Room room;
		Booking booking;
		
		List<Booking> bookings = new ArrayList<>();
		
		for (BookingRequest request : requests) {
			user = userRepository.findById(request.getUserId()).orElseThrow();
			room = roomRepository.findById(request.getRoomId()).orElseThrow();
			
			booking = Booking.builder()
				          .room(room)
				          .userId(user)
				          .startsAt(request.getStartsAt())
				          .endsAt(request.getEndsAt())
				          .purpose(request.getPurpose())
				          .status(request.getStatus())
				          .build();
			
			try {
				bookingRepository.save(booking);
			} catch (DataIntegrityViolationException e) {
				LOGGER.log(Level.WARNING, "Data Integrity Violation", e);
				continue;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			bookings.add(booking);
		}
		
		if (!bookings.isEmpty()) {
			return ResponseEntity.ok(bookings);
		} else {
			return ResponseEntity.notFound()
				       .build();
		}
	}
	
	@GetMapping("/me")
	public ResponseEntity<?> getMyBookings(@AuthenticationPrincipal UserDetails userDetails) {
		Long id = userRepository.findByName(userDetails.getUsername()).orElseThrow().getId();
		
		List<Booking> bookings = bookingRepository.findAll().stream()
			                         .filter(booking -> Objects.equals(booking.getUserId().getId(), id))
			                         .toList();
		
		return ResponseEntity.ok(bookings);
	}
	
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<?> deleteBooking(@PathVariable Long id,
	                                       @AuthenticationPrincipal UserDetails userDetails) {
		Booking booking = bookingRepository.findById(id).orElseThrow(
			() -> new BookingNotFoundException("Booking with id {} not found.", id)
		);
		
		List<String> authList = userDetails.getAuthorities().stream()
			                        .map(GrantedAuthority::getAuthority)
			                        .toList();
		
		if (booking.getUserId().getName().equals(userDetails.getUsername()) ||
		    authList.contains("ROLE_ADMIN")) {
			bookingRepository.delete(booking);
			return ResponseEntity.ok(booking);
		}
		
		return ResponseEntity.notFound()
			       .build();
	}
}
