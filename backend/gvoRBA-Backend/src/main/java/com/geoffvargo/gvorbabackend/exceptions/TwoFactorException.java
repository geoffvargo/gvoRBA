package com.geoffvargo.gvorbabackend.exceptions;

import org.springframework.http.*;

import java.awt.print.*;

import lombok.*;

@Getter
@RequiredArgsConstructor
public class TwoFactorException extends RuntimeException {
	private final ErrorCode code;
	private final HttpStatus status;
}
