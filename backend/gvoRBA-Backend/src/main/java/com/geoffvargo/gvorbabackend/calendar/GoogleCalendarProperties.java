package com.geoffvargo.gvorbabackend.calendar;

import org.springframework.boot.context.properties.*;

@ConfigurationProperties(prefix = "app.google-calendar")
public record GoogleCalendarProperties(
	Boolean enabled,
	String calendarId,
	String credentialsPath,
	String timeZone,
	String applicationName) {
}
