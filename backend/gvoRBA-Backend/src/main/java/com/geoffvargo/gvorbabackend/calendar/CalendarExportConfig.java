package com.geoffvargo.gvorbabackend.calendar;

import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;

/**
 * Registers {@link CalendarExportProperties} independently of Google Calendar sync, since ICS export
 * must work even when {@code app.google-calendar.enabled} is false.
 */
@Configuration
@EnableConfigurationProperties(CalendarExportProperties.class)
public class CalendarExportConfig {}