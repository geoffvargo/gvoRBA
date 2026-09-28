package com.geoffvargo.gvorbabackend.calendar;

import java.time.*;

public record IcsEvent(
	String uid,
	Instant start,
	Instant end,
	String summary,
	String description,
	Instant updated) {
}
