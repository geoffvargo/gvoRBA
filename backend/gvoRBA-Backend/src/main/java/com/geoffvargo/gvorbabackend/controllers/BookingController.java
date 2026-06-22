package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.logging.*;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "http://localhost:4200")
public class BookingController {
	public static final Logger LOGGER = Logger.getLogger(BookingController.class.getName());
	
	@Autowired
	private BookingRepository bookingRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	RoomRepository roomRepository;
	
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
		User user = userRepository.findById(request.getUserId()).orElseThrow();
		
		Room room = roomRepository.findById(request.getRoomId()).orElseThrow();
		
		Booking booking = Booking.builder()
			                  .roomId(room)
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
				          .roomId(room)
				          .userId(user)
				          .startsAt(request.getStartsAt())
				          .endsAt(request.getEndsAt())
				          .purpose(request.getPurpose())
				          .status(request.getStatus())
				          .build();
			
			bookingRepository.save(booking);
			bookings.add(booking);
		}
		
		return ResponseEntity.ok(bookings);
	}
}
