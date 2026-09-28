package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.calendar.*;

import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.format.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.*;

import java.io.*;
import java.nio.charset.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;

import lombok.*;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.google-calendar", name = "enabled", havingValue = "true")
public class CalendarExportController {
	public static final Logger LOGGER = LoggerFactory.getLogger(CalendarExportController.class);
	
	/**
	 * Guards against unbounded Google reads. Adjust as needed.
	 */
	public static final long MAX_RANGE_DAYS = 366;
	
	/**
	 * RFC 5545 8.1: the registered media type is text/calendar.
	 */
	public static final MediaType TEXT_CALENDAR = new MediaType("text", "calendar", StandardCharsets.UTF_8);
	
	private final CalendarExportService calendarExportService;
	
	private final GoogleCalendarProperties properties;
	
	/**
	 * Downloads the bookings in a date range as an iCalendar ({@code .ics}) file.
	 *
	 * <p>Reads the events from Google Calendar with
	 * {@link CalendarExportService#listEvents(LocalDate, LocalDate)}, serializes them with
	 * {@link IcsWriter#write(List, String)}, and returns the result as a {@link #TEXT_CALENDAR}
	 * attachment named {@code bookings-<from>-to-<to>.ics}, so browsers save it rather than
	 * display it.
	 *
	 * <p>Example: {@code GET /api/calendar/export.ics?from=2026-10-01&to=2026-10-31}
	 *
	 * @param from
	 * 	the first day of the range, inclusive, as an ISO date ({@code yyyy-MM-dd})
	 * @param to
	 * 	the last day of the range, inclusive, as an ISO date ({@code yyyy-MM-dd})
	 * @return a {@code 200 OK} response whose body is the UTF-8 encoded iCalendar document
	 * @throws ResponseStatusException
	 * 	with {@code 400 Bad Request} if {@code to} is before {@code from} or the range covers
	 * 	more than {@link #MAX_RANGE_DAYS} days (counting both ends), or with {@code 502 Bad Gateway} if Google Calendar
	 * 	cannot be read
	 */
	@GetMapping("/api/calendar/export.ics")
	public ResponseEntity<?> exportIcs(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
	                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		/// Reject reversed or oversized ranges before calling Google. DAYS.between excludes the
		/// end day, so ">= MAX_RANGE_DAYS" caps the inclusive range at MAX_RANGE_DAYS days.
		if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"'to' must be on or after 'from', and the range at most " + MAX_RANGE_DAYS + " days");
		}

		/// Fetch the events from Google. An API failure is an upstream problem, not the
		/// client's fault, so it is reported as 502 Bad Gateway rather than a generic 500.
		List<IcsEvent> events;
		try {
			events = calendarExportService.listEvents(from, to);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not read the Google calendar", e);
		}

		/// PRODID uses the RFC 5545 formal public identifier form: -//Owner//Product//Language.
		String productId = "-//" + properties.applicationName() + "//Booking Export//EN";

		/// Encode explicitly as UTF-8 so the bytes match the charset declared in TEXT_CALENDAR.
		byte[] body = IcsWriter.write(events, productId).getBytes(StandardCharsets.UTF_8);

		/// "attachment" makes browsers download the file instead of trying to display it.
		/// LocalDate.toString() is ISO format, e.g. bookings-2026-10-01-to-2026-10-31.ics.
		String filename = "bookings-" + from + "-to-" + to + ".ics";
		ContentDisposition disposition = ContentDisposition.attachment()
			                                 .filename(filename)
			                                 .build();

		LOGGER.info("/export.ics successfully hit!");

		/// Return the raw bytes so the body is sent exactly as encoded above, whichever message
		/// converter handles it.
		return ResponseEntity.ok()
			       .contentType(TEXT_CALENDAR)
			       .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
			       .body(body);
	}
}
