package com.geoffvargo.gvorbabackend.calendar;

import com.google.api.client.googleapis.json.*;
import com.google.api.client.util.*;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;

import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.event.*;

import java.io.*;
import java.time.*;

/**
 * Mirrors booking changes into a Google Calendar.
 *
 * <p>Listens for {@link BookingCalendarEvent}s published by the booking flow and, once the
 * surrounding transaction has committed, creates, updates, or deletes the matching Google
 * Calendar event. Each booking maps to exactly one calendar event whose ID is derived from the
 * booking ID, so repeated syncs of the same booking are idempotent.
 *
 * <p>The bean is only registered when {@code app.google-calendar.enabled} is {@code true},
 * matching the condition on {@link GoogleCalendarConfig}, which supplies the {@link Calendar}
 * client.
 */
@Service
@ConditionalOnProperty(prefix = "app.google-calendar", name = "enabled", havingValue = "true")
public class GoogleCalendarSyncService {
	public static final Logger LOGGER = LoggerFactory.getLogger(GoogleCalendarSyncService.class);

	/// Prefix for deterministic Google event IDs. Google only allows base32hex characters
	/// (lowercase {@code a-v} and {@code 0-9}) in event IDs, so this must stay within that set.
	private static final String EVENT_ID_PREFIX = "booking";

	private final Calendar calendar;

	private final GoogleCalendarProperties properties;

	/**
	 * Creates the sync service.
	 *
	 * @param calendar   authenticated Google Calendar API client
	 * @param properties Google Calendar settings (target calendar ID, time zone, etc.)
	 */
	public GoogleCalendarSyncService(Calendar calendar, GoogleCalendarProperties properties) {
		this.calendar = calendar;
		this.properties = properties;
	}

	/**
	 * Syncs a single booking change to Google Calendar.
	 *
	 * <p>Runs asynchronously after the publishing transaction commits, so a rolled-back booking
	 * never reaches Google and a slow API call never blocks the HTTP request. If no transaction
	 * is active, {@code fallbackExecution} lets the listener run anyway. Failures are logged
	 * rather than propagated, since the booking itself has already been saved.
	 *
	 * @param event the booking change to mirror
	 */
	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onBookingChanged(BookingCalendarEvent event) {
		String eventId = eventIdFor(event.bookingId());

		try {
			/// Cancelled bookings are removed from the calendar; everything else is created or
			/// refreshed in place.
			if (event.cancelled()) {
				delete(eventId);
			} else {
				upsert(eventId, event);
			}
		} catch (IOException e) {
			/// Swallow and log: the booking is already committed, and there is no caller left
			/// to handle the error on this async thread.
			LOGGER.error("Google Calendar sync failed for booking {}", event.bookingId(), e);
		}
	}

	/**
	 * Builds the deterministic Google Calendar event ID for a booking.
	 *
	 * @param bookingId the booking's database ID
	 * @return the event ID used for that booking in Google Calendar
	 */
	static String eventIdFor(long bookingId) {
		return EVENT_ID_PREFIX + bookingId;
	}

	/**
	 * Creates the calendar event for a booking, or updates it if it already exists.
	 *
	 * <p>Attempts an insert first; a {@code 409 Conflict} means an event with this ID already
	 * exists, in which case the error is ignored and the update below overwrites it.
	 *
	 * @param eventId the deterministic Google event ID for the booking
	 * @param source  the booking data to write
	 * @throws IOException if the Google API call fails for any reason other than a conflict
	 */
	private void upsert(String eventId, BookingCalendarEvent source) throws IOException {
		Event googleEvent = toGoogleEvent(eventId, source);
		String calendarId = properties.calendarId();

		try {
			calendar.events().insert(calendarId, googleEvent).execute();
			LOGGER.info("{} inserted successfully.", googleEvent.toPrettyString());
		} catch (GoogleJsonResponseException e) {
			/// 409 = event ID already taken, i.e. this booking was synced before. Anything
			/// else is a real failure.
			if (e.getStatusCode() != 409) {
				throw e;
			}
		}
		/// Overwrite the event with the latest booking data. Note this also runs right after a
		/// successful insert, not only after a 409.
		calendar.events().update(calendarId, eventId, googleEvent).execute();
	}

	/**
	 * Deletes the calendar event for a booking, if it exists.
	 *
	 * @param eventId the deterministic Google event ID for the booking
	 * @throws IOException if the Google API call fails for any reason other than the event
	 *                     already being missing
	 */
	private void delete(String eventId) throws IOException {
		try {
			calendar.events().delete(properties.calendarId(), eventId).execute();
		} catch (GoogleJsonResponseException e) {
			/// 404 (never created) and 410 (already deleted) both mean the event is gone, which
			/// is the desired end state, so treat them as success.
			int status = e.getStatusCode();
			if (status != 404 && status != 410) {
				throw e;
			}
		}
	}

	/**
	 * Converts a booking into a Google Calendar {@link Event}.
	 *
	 * @param eventId the deterministic Google event ID for the booking
	 * @param source  the booking data to convert
	 * @return a Google event populated with the booking's ID, title, description, and times
	 */
	private Event toGoogleEvent(String eventId, BookingCalendarEvent source) {
		/// Booking times are zone-less {@link LocalDateTime}s; interpret them in the configured
		/// calendar time zone.
		ZoneId zone = ZoneId.of(properties.timeZone());
		return new Event().setId(eventId)
			       .setSummary(source.summary())
			       .setDescription(source.description())
			       .setStart(toEventDateTime(source.start(), zone))
			       .setEnd(toEventDateTime(source.end(), zone));
	}

	/**
	 * Converts a local date-time into a Google {@link EventDateTime} in the given zone.
	 *
	 * @param value the wall-clock time to convert
	 * @param zone  the zone {@code value} is expressed in
	 * @return an {@link EventDateTime} carrying both the absolute instant and the zone ID
	 */
	private EventDateTime toEventDateTime(LocalDateTime value, ZoneId zone) {
		/// Pin the wall-clock time to an absolute instant, since Google's {@link DateTime}
		/// is epoch-based.
		long epochMillis = value.atZone(zone).toInstant().toEpochMilli();

		/// Also send the zone ID so Google displays the event in that zone rather than UTC.
		return new EventDateTime()
			       .setDateTime(new DateTime(epochMillis))
			       .setTimeZone(zone.getId());
	}
}