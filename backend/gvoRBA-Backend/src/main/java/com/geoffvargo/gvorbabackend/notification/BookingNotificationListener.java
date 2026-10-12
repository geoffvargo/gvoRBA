package com.geoffvargo.gvorbabackend.notification;

import com.geoffvargo.gvorbabackend.calendar.*;

import org.springframework.mail.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.event.*;

import java.nio.charset.*;
import java.util.*;

import lombok.*;
import lombok.extern.slf4j.*;

/**
 * Sends booking notification emails in response to booking events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingNotificationListener {
	private final EmailService emailService;
	private final BookingIcsService icsService;
	
	/**
	 * Emails the booker a confirmation with the booking attached as an {@code .ics} file.
	 * <p>
	 * Runs on an async thread after the booking transaction commits, so a slow or failing
	 * mail server never delays or rolls back the booking. Failures are logged, not rethrown.
	 *
	 * @param event
	 * 	the confirmed booking's details
	 */
	@Async
	/// fallbackExecution: the controller publishes outside a transaction; without it the event would be silently dropped.
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onBookingConfirmed(BookingConfirmedEvent event) {
		List<EmailAttachment> attachments = new ArrayList<>();
		
		/// The attachment is optional: if ICS generation fails, send the email without it.
		try {
			String ics = icsService.exportIcs(event.bookingId(), event.name(), true);
			attachments.add(new EmailAttachment(
				"booking-" + event.bookingId() + ".ics",
				ics.getBytes(StandardCharsets.UTF_8),
				"text/calendar; charset=UTF-8"
			));
		} catch (RuntimeException e) {
			log.warn("Booking {}: ICS not generated, sending without attachment.", event.bookingId(), e);
		}
		
		try {
			emailService.sendHtml(
				event.email(),
				"Booking Confirmed: " + event.roomName(),
				"email/booking-confirmed",
				Map.of(
					"guestName",
					event.name(),
					"roomName",
					event.roomName(),
					"start",
					event.start(),
					"end",
					event.end(),
					"bookingId",
					event.bookingId()
				),
				attachments
			);
		} catch (MailException e) {
			log.error("Booking {} confirmation email failed.", event.bookingId(), e);
		}
	}
}
