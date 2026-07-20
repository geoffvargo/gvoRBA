package com.geoffvargo.gvorbabackend.exceptions;

import org.flywaydb.core.api.*;
import org.springframework.http.*;

import lombok.*;

@Getter
public class OverlapConflictException extends RuntimeException {
	public OverlapConflictException(ErrorCode code, HttpStatus status, String message) {
		super(message);
	}
	
}
