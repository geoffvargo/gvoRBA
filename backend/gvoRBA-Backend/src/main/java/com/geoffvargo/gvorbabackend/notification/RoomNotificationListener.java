package com.geoffvargo.gvorbabackend.notification;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;

import org.springframework.mail.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.event.*;

import java.util.*;

import lombok.*;
import lombok.extern.slf4j.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomNotificationListener {
	private final EmailService emailService;
	
	private final UserPrefsRepository prefsRepository;
	
	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onRoomDeactivated(RoomDeactivatedEvent event) {
		List<User> users = prefsRepository.findRoomDeactivationRecipients(event.roomId());
		
		log.info("onRoomDeactived() called.");
		
		try {
			for (User user : users) {
				if (user.getUserPrefs().getEmailNotification() && user.getUserPrefs().getNotifyOnRoomDeactivated()) {
					log.info("email sent.");
					emailService.sendHtml(
						user.getEmail(),
						"Room Deactivated: " + event.roomName(),
						"email/room-deactivated",
						Map.of(
							"userName",
							user.getName(),
							"roomName",
							event.roomName(),
							"location",
							event.location()
						),
						List.of()
					);
				}
			}
		} catch (MailException e) {
			log.error("Email generation failed.", e);
		}
	}
	
	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onRoomReactivated(RoomReactivatedEvent event) {
		List<User> users = prefsRepository.findRoomDeactivationRecipients(event.roomId());
		
		log.info("onRoomReactivated() called.");
		
		try {
			for (User user : users) {
				if (user.getUserPrefs().getEmailNotification() && user.getUserPrefs().getNotifyOnRoomDeactivated()) {
					log.info("sending email");
					emailService.sendHtml(
						user.getEmail(),
						"Room Reactivated: " + event.roomName(),
						"email/room-reactivated",
						Map.of(
							"userName",
							user.getName(),
							"roomName",
							event.roomName(),
							"location",
							event.location()
						),
						List.of()
					);
				}
			}
		} catch (MailException e) {
			log.error("Email generation failed.", e);
		}
	}
}
