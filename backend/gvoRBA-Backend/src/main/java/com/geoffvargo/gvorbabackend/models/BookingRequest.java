package com.geoffvargo.gvorbabackend.models;

import java.time.*;
import java.util.*;

import javax.annotation.*;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequest {
	private Long roomId;
	private Long userId;
	private LocalDateTime startsAt;
	private LocalDateTime endsAt;
	private String purpose;
	private BookingStatus status;
	
	@Nullable
	private Set<Long> attendees;
}
