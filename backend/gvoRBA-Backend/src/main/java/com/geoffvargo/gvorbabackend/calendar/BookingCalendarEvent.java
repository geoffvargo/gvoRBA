package com.geoffvargo.gvorbabackend.calendar;

import java.time.*;

public record BookingCalendarEvent(
	long bookingId,
	String summary,
	String description,
	LocalDateTime start,
	LocalDateTime end,
	Boolean cancelled) {
}
