package com.geoffvargo.gvorbabackend.notification;

import java.util.*;

public record RoomDeactivatedEvent(
	Long roomId,
	String roomName,
	String location,
	Integer capacity,
	List<String> amenities
) {}
