package com.geoffvargo.gvorbabackend.calendar;

import java.time.*;

import javax.annotation.*;

import lombok.*;

@Builder
public record IcsEntry(
	String uid,
	@Nullable Long roomId,
	Instant start,
	Instant end,
	String summary,
	Instant updated
) {}
