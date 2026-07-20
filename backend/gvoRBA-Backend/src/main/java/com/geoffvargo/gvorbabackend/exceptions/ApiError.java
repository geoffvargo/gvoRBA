package com.geoffvargo.gvorbabackend.exceptions;

import com.fasterxml.jackson.annotation.*;

import org.flywaydb.core.api.*;

import java.time.*;

public record ApiError(
	@JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,
	int status,
	ErrorCode code,
	String message,
	String path
) {
}
