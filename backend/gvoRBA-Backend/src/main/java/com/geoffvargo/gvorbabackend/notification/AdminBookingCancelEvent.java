package com.geoffvargo.gvorbabackend.notification;

import java.time.*;

public record AdminBookingCancelEvent(
	Long bookingId,
	String email,
	String name,
	String adminName,
	String roomName,
	LocalDateTime start,
	LocalDateTime end
) {}
