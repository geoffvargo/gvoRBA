package com.geoffvargo.gvorbabackend.calendar;

import org.springframework.boot.context.properties.*;

@ConfigurationProperties(prefix = "app.calendar-export")
public record CalendarExportProperties(
	String timeZone,
	String uidDomain,
	String productName
) {}
