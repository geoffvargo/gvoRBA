package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.*;
import com.geoffvargo.gvorbabackend.calendar.*;
import com.geoffvargo.gvorbabackend.exceptions.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.models.User;
import com.geoffvargo.gvorbabackend.repos.*;

import org.springframework.context.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.security.core.*;
import org.springframework.security.core.annotation.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.*;
import java.time.*;
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
	
	private final ApplicationEventPublisher appEventPublisher;
	
	private final BookingIcsService bookingIcsService;
	
	/**
	 * RFC 5545 8.1: the registered media type is text/calendar.
	 */
	public static final MediaType TEXT_CALENDAR = new MediaType("text", "calendar", StandardCharsets.UTF_8);
	
	private void publishCalendarEvent(Booking booking, Boolean cancelled) {
		String desciption = Objects.toString(booking.getPurpose(), "") +
		                    "\nBooked by: " + booking.getUserId().getName();
		
		LOGGER.info(desciption);
		
		appEventPublisher.publishEvent(
			new BookingCalendarEvent(
				booking.getId(),
				booking.getRoom().getName(),
				desciption,
				booking.getStartsAt(),
				booking.getEndsAt(),
				cancelled,
				booking.getRoom().getId()
			)
		);
	}
	
	@GetMapping("/{id}/calendar.ics")
	public ResponseEntity<byte[]> downloadIcs(@PathVariable Long id,
	                                          @AuthenticationPrincipal UserDetails userDetails) {
		String username = userDetails.getUsername();
		
		byte[] ans = bookingIcsService.exportIcs(id, username, true).getBytes();
		
		ContentDisposition disposition = ContentDisposition.attachment()
			                                 .filename("bookings-" + id + ".ics")
			                                 .build();
		
		return ResponseEntity.ok()
			       .contentType(TEXT_CALENDAR)
			       .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
			       .body(ans);
	}
	
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
		
		Set<User> attendees = request.getAttendees() == null
		                      ? new HashSet<>()
		                      : new HashSet<>(userRepository.findAllById(request.getAttendees()));
		
		Booking booking = Booking.builder()
			                  .room(room)
			                  .userId(user)
			                  .startsAt(request.getStartsAt())
			                  .endsAt(request.getEndsAt())
			                  .purpose(request.getPurpose())
			                  .status(request.getStatus())
			                  .attendees(attendees)
			                  .build();
		
		try {
			bookingRepository.save(booking);
			publishCalendarEvent(booking, false);
		} catch (DataIntegrityViolationException e) {
			throw new OverlapConflictException(ErrorCode.BOOKING_CONFLICT, HttpStatus.CONFLICT, e.getMessage());
		}
		
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
				publishCalendarEvent(booking, false);
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
			publishCalendarEvent(booking, true);
			return ResponseEntity.ok(booking);
		}
		
		return ResponseEntity.notFound()
			       .build();
	}
	
	@PatchMapping("/cancel/{id}")
	public ResponseEntity<?> cancelBooking(@PathVariable Long id,
	                                       @AuthenticationPrincipal UserDetails userDetails) {
		Booking booking = bookingRepository.findById(id).orElseThrow(
			() -> new BookingNotFoundException("Booking with id {} not found.", id)
		);
		
		List<String> authList = userDetails.getAuthorities().stream()
			                        .map(GrantedAuthority::getAuthority)
			                        .toList();
		
		if (booking.getUserId().getName().equals(userDetails.getUsername()) ||
		    authList.contains("ROLE_ADMIN")) {
			booking.setStatus(BookingStatus.CANCELLED);
			booking.setCancelledAt(LocalDateTime.now());
			bookingRepository.save(booking);
			publishCalendarEvent(booking, true);
			return ResponseEntity.ok(booking);
		}
		
		throw new SecurityException("You do not have the appropriate rights to cancel this booking");
	}
	
	@PatchMapping("/uncancel/{id}")
	public ResponseEntity<?> uncancelBooking(@PathVariable Long id,
	                                         @AuthenticationPrincipal UserDetails userDetails) {
		Booking booking = bookingRepository.findById(id).orElseThrow(
			() -> new BookingNotFoundException("Booking with id {} not found.", id)
		);
		
		List<String> authList = userDetails.getAuthorities().stream()
			                        .map(GrantedAuthority::getAuthority)
			                        .toList();
		
		if (booking.getUserId().getName().equals(userDetails.getUsername()) ||
		    authList.contains("ROLE_ADMIN")) {
			booking.setStatus(BookingStatus.CONFIRMED);
			booking.setCancelledAt(null);
			bookingRepository.save(booking);
			publishCalendarEvent(booking, false);
			
			return ResponseEntity.ok(booking);
		}
		
		throw new SecurityException("You do not have the appropriate rights to uncancel this booking");
	}
}
