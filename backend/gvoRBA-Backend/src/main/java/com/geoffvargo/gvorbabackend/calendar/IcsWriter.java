package com.geoffvargo.gvorbabackend.calendar;

import com.geoffvargo.gvorbabackend.models.*;

import java.time.*;
import java.time.format.*;
import java.util.*;

import lombok.*;

/**
 * Writes a minimal RFC 5545 iCalendar document.
 */
@NoArgsConstructor
public final class IcsWriter {
	/**
	 * RFC 5545 3.1: lines end in CRLF, not just LF.
	 */
	public static final String CRLF = "\r\n";
	
	/**
	 * RFC 5545 3.1: lines SHOULD NOT exceed 75 octets
	 * (bytes, not characters), excluding the CRLF.
	 */
	public static final int MAX_LINE_OCTETS = 75;
	
	/**
	 * RFC 5545 3.3.5 UTC form, e.g. 20261001T140000Z. UTC
	 */
	public static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ofPattern("yyyMMdd'T'HHmmss'Z'")
		                                                   .withZone(ZoneOffset.UTC);
	
	/**
	 * Serializes a list of events into a complete iCalendar ({@code .ics}) document.
	 *
	 * <p>Produces a single {@code VCALENDAR} with {@code VERSION}, {@code PRODID}, and
	 * {@code CALSCALE} headers, followed by one {@code VEVENT} per entry in {@code events}. Each
	 * event carries {@code UID}, {@code DTSTAMP}, {@code DTSTART}, {@code DTEND}, and
	 * {@code SUMMARY}; {@code DESCRIPTION} is included only when the event has a non-empty
	 * description.
	 *
	 * <p>All times are written in UTC form using {@link #UTC_FORMAT}, text values are escaped
	 * with {@link #escapeText(String)}, and every line is folded and CRLF-terminated by
	 * {@link #line(StringBuilder, String)}, so the output conforms to RFC 5545.
	 *
	 * @param events
	 * 	the events to include, in output order; may be empty, which yields a calendar with no
	 * 	events
	 * @param productId
	 * 	the {@code PRODID} value identifying the software that generated the calendar, e.g.
	 * 	{@code -//gvoRBA//Bookings//EN}
	 * @return the full iCalendar document as a string
	 */
	public static String write(List<IcsEvent> events, String productId) {
		StringBuilder out = new StringBuilder();
		
		line(out, "BEGIN:VCALENDAR");
		line(out, "VERSION:2.0");
		line(out, "PRODID:" + escapeText(productId));
		line(out, "CALSCALE:GREGORIAN");
		
		for (IcsEvent event : events) {
			line(out, "BEGIN:VEVENT");
			
			/// UID and DTSTAMP are required (RFC 5545 3.6.1).
			line(out, "UID:" + escapeText(event.uid()));
			
			/// With no METHOD property, DTSTAMP means "last modified" (RFC 5545 3.8.7.2).
			line(out, "DTSTAMP:" + UTC_FORMAT.format(event.updated()));
			line(out, "DTSTART:" + UTC_FORMAT.format(event.start()));
			line(out, "DTEND:" + UTC_FORMAT.format(event.end()));
			line(out, "SUMMARY:" + escapeText(event.summary()));
			
			String description = event.description();
			if (description != null && !description.isEmpty()) {
				line(out, "DESCRIPTION:" + escapeText(description));
			}
			
			line(out, "END:VEVENT");
		}
		
		line(out, "END:VCALENDAR");
		
		return out.toString();
	}
	
	public static String write(IcsEntry event, String productId) {
		StringBuilder out = new StringBuilder();
		
		line(out, "BEGIN:VCALENDAR");
		line(out, "VERSION:2.0");
		line(out, "PRODID:" + escapeText(productId));
		line(out, "CALSCALE:GREGORIAN");
		line(out, "BEGIN:VEVENT");
		
		line(out, "UID:" + escapeText(event.uid()));
		
		if (event.roomId() != null) {
			line(out, "X-GVRB-ROOM-ID:" + event.roomId());
		}
		
		line(out, "DTSTAMP:" + UTC_FORMAT.format(event.updated()));
		line(out, "DTSTART:" + UTC_FORMAT.format(event.start()));
		line(out, "DTEND:" + UTC_FORMAT.format(event.end()));
		line(out, "SUMMARY:" + escapeText(event.summary()));
		
		line(out, "END:VEVENT");
		line(out, "END:VCALENDAR");
		
		return out.toString();
	}
	
	/**
	 * Escapes a string for use as an iCalendar {@code TEXT} property value.
	 *
	 * <p>Implements RFC 5545 section 3.3.11: backslash, semicolon, and comma are each prefixed
	 * with a backslash, and every line break ({@code CRLF}, {@code LF}, or a lone {@code CR})
	 * becomes the two-character sequence {@code \n}.
	 *
	 * <p>Replacement order matters. Backslashes are escaped first so the backslashes added by
	 * the later replacements are not doubled, and {@code CRLF} is handled before {@code LF} and
	 * {@code CR} so a Windows line break turns into one {@code \n} rather than two.
	 *
	 * @param value
	 * 	the raw text to escape; may be {@code null}
	 * @return the escaped text, or {@code null} if {@code value} is {@code null}
	 */
	private static String escapeText(String value) {
		if (value == null) {
			return null;
		}
		
		return value.replace("\\", "\\\\")
			       .replace(";", "\\;")
			       .replace(",", "\\,")
			       .replace("\r\n", "\\n")
			       .replace("\n", "\\n")
			       .replace("\r", "\\n");
	}
	
	/**
	 * Appends one content line to the iCalendar output.
	 *
	 * <p>Folds {@code str} with {@link #fold(String)} so no physical line exceeds
	 * {@link #MAX_LINE_OCTETS}, then terminates it with {@link #CRLF}, as RFC 5545 section 3.1
	 * requires.
	 *
	 * @param out
	 * 	the buffer the calendar document is being built in
	 * @param str
	 * 	the unfolded content line, without a trailing line break
	 */
	private static void line(StringBuilder out, String str) {
		out.append(fold(str)).append(CRLF);
	}
	
	/**
	 * Folds a content line so that no physical line exceeds {@link #MAX_LINE_OCTETS}.
	 *
	 * <p>Implements RFC 5545 section 3.1: whenever the next character would push the current
	 * physical line past the limit, a {@link #CRLF} followed by a single space is inserted before
	 * it. Readers undo the fold by removing each CRLF-plus-whitespace pair.
	 *
	 * <p>Length is measured in UTF-8 octets via {@link #utfLength(int)}, not in Java
	 * {@code char}s, and the line is walked one code point at a time, so a multibyte character
	 * (including a surrogate pair) is never split across a fold. The leading space of each
	 * continuation line counts toward that line's limit.
	 *
	 * @param line
	 * 	the unfolded content line, without a trailing line break
	 * @return {@code line} with fold sequences inserted where needed; unchanged if it already fits
	 * 	within {@link #MAX_LINE_OCTETS}
	 */
	private static String fold(String line) {
		StringBuilder out = new StringBuilder();
		int octets = 0;
		int i = 0;
		
		while (i < line.length()) {
			int codePoint = line.codePointAt(i);
			int size = utfLength(codePoint);
			
			if (octets + size > MAX_LINE_OCTETS) {
				out.append(CRLF).append(' ');
				octets = 1;
			}
			
			out.appendCodePoint(codePoint);
			octets += size;
			i += Character.charCount(codePoint);
		}
		
		return out.toString();
	}
	
	/**
	 * Returns the number of bytes a Unicode code point occupies when encoded as UTF-8.
	 *
	 * <p>Used by {@link #fold(String)} to measure line length in octets, as RFC 5545 requires,
	 * rather than in Java {@code char}s. The ranges follow the UTF-8 encoding rules:
	 * <ul>
	 *   <li>{@code U+0000}–{@code U+007F}: 1 byte (ASCII)</li>
	 *   <li>{@code U+0080}–{@code U+07FF}: 2 bytes</li>
	 *   <li>{@code U+0800}–{@code U+FFFF}: 3 bytes</li>
	 *   <li>{@code U+10000}–{@code U+10FFFF}: 4 bytes (supplementary characters, e.g. emoji)</li>
	 * </ul>
	 *
	 * @param codePoint
	 * 	a valid Unicode code point, as returned by {@link String#codePointAt(int)}
	 * @return the UTF-8 encoded length of {@code codePoint}, from 1 to 4
	 */
	private static int utfLength(int codePoint) {
		if (codePoint < 0x80) {
			return 1;
		} else if (codePoint < 0x800) {
			return 2;
		}
		
		return codePoint < 0x10000 ? 3 : 4;
	}
}
