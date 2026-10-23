package com.geoffvargo.gvorbabackend.notification;

import java.time.*;

public record BookingCancelledEvent(
	Long bookingId,
	String email,
	String name,
	String roomName,
	LocalDateTime start,
	LocalDateTime end
) {}
