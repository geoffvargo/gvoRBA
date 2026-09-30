package com.geoffvargo.gvorbabackend.calendar;

import com.geoffvargo.gvorbabackend.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;

import org.slf4j.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.nio.charset.*;
import java.time.*;
import java.util.*;

import lombok.*;

@Service
@RequiredArgsConstructor
public class BookingIcsService {
	public static final Logger LOGGER = LoggerFactory.getLogger(BookingIcsService.class);
	
	private final BookingRepository bookingRepository;
	
	private final CalendarExportProperties properties;
	
	@Transactional(readOnly = true)
	public String exportIcs(Long bookingId, String requesterName, boolean isAdmin) {
		Booking booking = bookingRepository.findById(bookingId).orElseThrow(
			() -> new BookingNotFoundException("Booking {} not found.", bookingId)
		);
		
		User owner = booking.getUserId();
		if (owner.getName().equalsIgnoreCase(requesterName) || isAdmin) {
			if (booking.getStatus().equals(BookingStatus.CONFIRMED)) {
				String uuidStr = properties.productName() + ":booking:" + booking.getId();
				IcsEntry entry = IcsEntry.builder()
					                 .uid(UUID.nameUUIDFromBytes(uuidStr.getBytes(StandardCharsets.UTF_8)).toString())
					                 .roomId(booking.getRoom().getId())
					                 .start(booking.getStartsAt().toInstant(ZoneOffset.UTC))
					                 .end(booking.getEndsAt().toInstant(ZoneOffset.UTC))
					                 .summary(booking.getPurpose())
					                 .updated(Instant.now())
					                 .build();
				
				return IcsWriter.write(entry, properties.productName());
			}
		}
		
		throw new RuntimeException("Not allowed to publish this boooking.");
	}
}
