package com.geoffvargo.gvorbabackend.calendar;

import com.google.api.client.util.*;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;

import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.time.*;
import java.util.*;

import lombok.*;

/**
 * Reads booking events back out of Google Calendar for export as iCalendar data.
 *
 * <p>Queries the calendar configured in {@link GoogleCalendarProperties}, the same one
 * {@link GoogleCalendarSyncService} writes to, and converts each timed event into an
 * {@link IcsEvent}, ready to be serialized with {@link IcsWriter#write(List, String)}.
 *
 * <p>The bean is only registered when {@code app.google-calendar.enabled} is {@code true},
 * matching the condition on {@link GoogleCalendarConfig}, which supplies the {@link Calendar}
 * client.
 */
@Service
@AllArgsConstructor
@ConditionalOnProperty(prefix = "app.google-calendar", name = "enabled", havingValue = "true")
public class CalendarExportService {
	/**
	 * Largest page events.list allows (default is 250).
	 */
	public static final int PAGE_SIZE = 2500;
	
	/// From the sync guide: GoogleCalendarConfig (2.4) and GoogleCalendarProperties (2.3).
	private final Calendar calendar;
	private final GoogleCalendarProperties properties;
	
	/**
	 * Fetches every Google Calendar event that overlaps the given date range.
	 *
	 * <p>The range covers {@code from} through {@code to}, both days inclusive, interpreted in the
	 * configured time zone: it runs from midnight at the start of {@code from} up to (but not
	 * including) midnight at the start of the day after {@code to}. Google returns any event that
	 * overlaps that window, so events that begin before {@code from} or end after {@code to} are
	 * included as long as part of them falls inside it.
	 *
	 * <p>Recurring events are expanded into individual instances, results are ordered by start
	 * time, and all result pages are fetched ({@link #PAGE_SIZE} events per request). Cancelled
	 * and all-day events are skipped; see {@link #toIcsEvent(Event)}.
	 *
	 * @param from
	 * 	the first day of the range, inclusive
	 * @param to
	 * 	the last day of the range, inclusive
	 * @return the matching events in start-time order; empty if none match
	 * @throws IOException
	 * 	if a request to the Google Calendar API fails
	 */
	public List<IcsEvent> listEvents(LocalDate from, LocalDate to) throws IOException {
		ZoneId zone = ZoneId.of(properties.timeZone());
		
		DateTime timeMin = toGoogle(from.atStartOfDay(zone).toInstant());
		DateTime timeMax = toGoogle(to.plusDays(1).atStartOfDay(zone).toInstant());
		
		List<IcsEvent> result = new ArrayList<>();
		String pageToken = null;
		do {
			Events page = calendar.events()
				              .list(properties.calendarId())
				              .setTimeMin(timeMin)
				              .setTimeMax(timeMax)
				              .setSingleEvents(true) /// Expand recurring events into instances.
				              .setOrderBy("startTime")
				              .setMaxResults(PAGE_SIZE)
				              .setPageToken(pageToken)
				              .execute();
			
			if (page.getItems() != null) {
				for (Event event : page.getItems()) {
					toIcsEvent(event).ifPresent(result::add);
				}
			}
			
			/// Null when there are no more pages.
			pageToken = page.getNextPageToken();
		} while (pageToken != null);
		
		return result;
	}
	
	/**
	 * Converts a Google Calendar {@link Event} into an {@link IcsEvent} for export.
	 *
	 * <p>Only timed events are converted. An event whose start or end is missing, or has no
	 * {@code dateTime} (i.e. an all-day event, which uses {@code date} instead), is skipped;
	 * {@link GoogleCalendarSyncService} never creates all-day events, so none are expected.
	 * Cancelled events never reach this method, because {@code showDeleted} defaults to
	 * {@code false} in {@link #listEvents(LocalDate, LocalDate)}.
	 *
	 * <p>The event's {@code iCalUID} is used as the {@code UID}, since it identifies the event
	 * consistently across calendar clients. If Google omits the last-modified time, the current
	 * time is used for {@code DTSTAMP} instead.
	 *
	 * @param event
	 * 	the Google Calendar event to convert
	 * @return the converted event, or {@link Optional#empty()} if it is not a timed event
	 */
	private Optional<IcsEvent> toIcsEvent(Event event) {
		EventDateTime start = event.getStart();
		EventDateTime end = event.getEnd();
		
		if (start == null || end == null || start.getDateTime() == null || end.getDateTime() == null) {
			return Optional.empty();
		}
		
		Instant updated = event.getUpdated() != null ? toInstant(event.getUpdated()) : Instant.now();
		
		return Optional.of(
			new IcsEvent(
				event.getICalUID(),
				toInstant(start.getDateTime()),
				toInstant(end.getDateTime()),
				event.getSummary(),
				event.getDescription(),
				updated)
		);
	}
	
	private Instant toInstant(DateTime dateTime) {
		return Instant.ofEpochMilli(dateTime.getValue());
	}
	
	private DateTime toGoogle(Instant instant) {
		return new DateTime(instant.toEpochMilli());
	}
}
