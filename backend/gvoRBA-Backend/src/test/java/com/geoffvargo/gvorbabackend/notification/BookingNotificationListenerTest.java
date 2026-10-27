package com.geoffvargo.gvorbabackend.notification;

import com.geoffvargo.gvorbabackend.calendar.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.mail.*;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BookingNotificationListener#onAdminCancelled}: it should send the
 * admin-cancellation email with the right template and model, and never let a mail failure escape.
 */
@ExtendWith(MockitoExtension.class)
class BookingNotificationListenerTest {

	@Mock private EmailService emailService;
	@Mock private BookingIcsService icsService;

	@InjectMocks private BookingNotificationListener listener;

	@Captor private ArgumentCaptor<Map<String, Object>> modelCaptor;
	@Captor private ArgumentCaptor<List<EmailAttachment>> attachmentsCaptor;

	private static final LocalDateTime START = LocalDateTime.of(2026, 10, 22, 10, 0);
	private static final LocalDateTime END = START.plusHours(1);

	private final AdminBookingCancelEvent event = new AdminBookingCancelEvent(
		42L, "pat@example.com", "Pat", "Admin Alex", "Board Room", START, END
	);

	@Test
	void onAdminCancelled_sendsEmailWithTemplateAndModel() {
		listener.onAdminCancelled(event);

		verify(emailService).sendHtml(
			eq("pat@example.com"),
			eq("Booking Cancelled by Admin"),
			eq("email/admin-cancelled-booking"),
			modelCaptor.capture(),
			attachmentsCaptor.capture()
		);

		assertThat(modelCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
			"userName", "Pat",
			"roomName", "Board Room",
			"start", START,
			"end", END,
			"bookingId", 42L
		));
		/// Cancellations carry no .ics, so the ICS service should never be asked for one.
		assertThat(attachmentsCaptor.getValue()).isEmpty();
		verifyNoInteractions(icsService);
	}

	@Test
	void onAdminCancelled_swallowsMailException() {
		// TODO(human): make sendHtml throw a MailException, call the listener, and assert the right outcome.
	}
}