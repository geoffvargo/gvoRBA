package com.geoffvargo.gvorbabackend.security;

import org.springframework.context.event.*;
import org.springframework.stereotype.*;

import lombok.extern.slf4j.*;

@Slf4j
@Component
public class TwoFactorEventLogger {
	@EventListener
	public void on(TwoFactorEvent event) {
		log.info("type: {}. username: {}, at: {}", event.type(), event.username(), event.at());
		throw new UnsupportedOperationException();
	}
}
