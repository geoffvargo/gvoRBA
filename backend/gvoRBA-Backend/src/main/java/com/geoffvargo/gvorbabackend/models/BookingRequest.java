package com.geoffvargo.gvorbabackend.models;

import java.time.*;
import java.util.*;
import java.util.stream.*;

import javax.annotation.*;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequest {
	private Long roomId;
	private Long userId;
	private LocalDateTime startsAt;
	private LocalDateTime endsAt;
	private String purpose;
	private BookingStatus status;
	
	@Nullable
	private Set<Long> attendees;
	
	public static BookingRequest fromBooking(Booking booking) {
		return new BookingRequest(booking.getRoom().getId(),
			booking.getUserId().getId(),
			booking.getStartsAt(),
			booking.getEndsAt(),
			booking.getPurpose(),
			booking.getStatus(),
			(booking.getAttendees() != null) ? booking.getAttendees().stream()
				                                   .map(User::getId)
				                                   .collect(Collectors.toSet()) : null);
	}
}
