package com.geoffvargo.gvorbabackend.calendar;

import com.google.api.client.http.javanet.*;
import com.google.api.client.json.gson.*;
import com.google.api.services.calendar.Calendar;
import com.google.auth.http.*;
import com.google.auth.oauth2.*;

import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Configuration
@EnableAsync
@EnableConfigurationProperties(GoogleCalendarProperties.class)
@ConditionalOnProperty(prefix = "app.google-calendar", name = "enabled", havingValue = "true")
public class GoogleCalendarConfig {
	public static final Logger LOGGER = LoggerFactory.getLogger(GoogleCalendarConfig.class);
	
	public static final String CALENDAR_EVENTS_SCOPE = "https://www.googleapis.com/auth/calendar.events";
	
	@Bean
	public Calendar googleCalendarClient(GoogleCalendarProperties properties)
	throws IOException {
		GoogleCredentials credentials;
		
		try (InputStream keyStream = Files.newInputStream(Path.of(properties.credentialsPath()))) {
			credentials = ServiceAccountCredentials
				              .fromStream(keyStream)
				              .createScoped(List.of(CALENDAR_EVENTS_SCOPE));
		}
		
		return new Calendar.Builder(
			new NetHttpTransport(),
			GsonFactory.getDefaultInstance(),
			new HttpCredentialsAdapter(credentials))
			       .setApplicationName(properties.applicationName())
			       .build();
	}
}
