package com.geoffvargo.gvorbabackend.notification;

import java.util.*;

public record RoomReactivatedEvent(
	Long roomId,
	String roomName,
	String location,
	Integer capacity,
	List<String> amenities
) {}
