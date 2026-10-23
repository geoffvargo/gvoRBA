package com.geoffvargo.gvorbabackend.notification;

import java.time.*;
import java.util.*;

public record BookingChangedEvent(
	Long bookingId,
	String email,
	String name,
	String roomName,
	LocalDateTime start,
	LocalDateTime end,
	List<String> changes
) {}
