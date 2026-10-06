package com.geoffvargo.gvorbabackend.exceptions;

public class BookingNotFoundException extends RuntimeException {
	public BookingNotFoundException(Long id) {
		super("Booking with id " + id + " not found.");
	}
}
