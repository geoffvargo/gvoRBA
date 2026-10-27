package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.calendar.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.notification.*;
import com.geoffvargo.gvorbabackend.repos.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.context.*;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BookingController#editBooking}'s change detection: the {@code changes}
 * list carried by the published {@link BookingChangedEvent} should name exactly the fields that differ.
 */
@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

	@Mock private BookingRepository bookingRepository;
	@Mock private UserRepository userRepository;
	@Mock private RoomRepository roomRepository;
	@Mock private ApplicationEventPublisher appEventPublisher;
	@Mock private BookingIcsService bookingIcsService;

	@InjectMocks private BookingController controller;

	private static final LocalDateTime START = LocalDateTime.of(2026, 10, 22, 10, 0);
	private static final LocalDateTime END = START.plusHours(1);

	private User user;
	private Room room;
	private Booking booking;

	/**
	 * Builds an existing booking and stubs the repositories so {@code editBooking} can find it.
	 */
	@BeforeEach
	void setUp() {
		user = User.builder().id(7L).email("pat@example.com").name("Pat").build();
		/// Turn on change notifications so the event is always published and can be captured.
		user.setUserPrefs(UserPrefs.builder().notifyOnBookingChange(true).build());

		room = Room.builder().id(3L).name("Blue Room").build();

		booking = Booking.builder()
			          .id(42L)
			          .room(room)
			          .userId(user)
			          .startsAt(START)
			          .endsAt(END)
			          .purpose("Standup")
			          .status(BookingStatus.values()[0])
			          .build();

		when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
		when(userRepository.findById(7L)).thenReturn(Optional.of(user));
		when(roomRepository.findById(3L)).thenReturn(Optional.of(room));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	/**
	 * Returns a request matching the existing booking exactly; tests tweak one field from here.
	 */
	private BookingRequest.BookingRequestBuilder unchangedRequest() {
		return BookingRequest.builder()
			       .roomId(3L)
			       .userId(7L)
			       .startsAt(START)
			       .endsAt(END)
			       .purpose("Standup")
			       .status(booking.getStatus());
	}

	/**
	 * Runs {@code editBooking} and returns the {@code changes} list from the published event.
	 */
	private List<String> editAndCaptureChanges(BookingRequest request) {
		controller.editBooking(42L, request);

		ArgumentCaptor<BookingChangedEvent> captor = ArgumentCaptor.forClass(BookingChangedEvent.class);
		verify(appEventPublisher).publishEvent(captor.capture());
		return captor.getValue().changes();
	}

	@Test
	void onlyPurposeChanged_reportsOnlyPurpose() {
		List<String> changes = editAndCaptureChanges(unchangedRequest().purpose("Retro").build());

		assertThat(changes).containsExactly("purpose");
	}

	@Test
	void nothingChanged_reportsNoChanges() {
		List<String> changes = editAndCaptureChanges(unchangedRequest().build());

		assertThat(changes).isEmpty();
	}
}