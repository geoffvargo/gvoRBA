package com.geoffvargo.gvorbabackend.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a shared {@link Clock} so time-dependent code can be tested with a fixed clock.
 */
@Configuration
public class ClockConfig {

	/**
	 * System clock in UTC. TOTP codes are based on Unix time, so the time zone doesn't matter.
	 */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
