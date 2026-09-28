package com.geoffvargo.gvorbabackend.calendar;

import com.geoffvargo.gvorbabackend.models.*;

import org.springframework.context.*;
import org.springframework.stereotype.*;

import java.util.*;

import lombok.*;

@Component
@RequiredArgsConstructor
public class BookingCalendarPublisher {
	private final ApplicationEventPublisher eventPublisher;
	
	public void publish(Booking booking, Boolean cancelled) {
		eventPublisher.publishEvent(
			new BookingCalendarEvent(
				booking.getId(),
				booking.getRoom().getName(),
				getDescription(booking),
				booking.getStartsAt(),
				booking.getEndsAt(),
				cancelled
			)
		);
	}
	
	private static @org.jspecify.annotations.NonNull String getDescription(Booking booking) {
		return Objects.toString(booking.getPurpose(), "") + "\nBooked by: " + booking.getUserId().getName();
	}
}
