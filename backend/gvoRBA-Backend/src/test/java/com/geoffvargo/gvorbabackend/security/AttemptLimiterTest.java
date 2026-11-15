package com.geoffvargo.gvorbabackend.security;

import com.geoffvargo.gvorbabackend.exceptions.*;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.time.*;
import java.time.temporal.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AttemptLimiter}. A {@link MutableClock} lets each test move time forward without sleeping.
 */
class AttemptLimiterTest {
	private static final String KEY = "user-1";
	private static final String OTHER_KEY = "user-2";
	private static final Instant START = Instant.parse("2026-11-02T12:00:00Z");
	
	private MutableClock clock;
	private AttemptLimiter limiter;
	
	@BeforeEach
	void setUp() {
		clock = new MutableClock(START);
		limiter = new AttemptLimiter(clock);
	}
	
	// --- tests ---
	
	@Test
	void checkAllowed_unknownKey_doesNotThrow() {
		assertDoesNotThrow(() -> limiter.checkAllowed(KEY));
	}
	
	@Test
	void checkAllowed_belowLimit_doesNotThrow() {
		recordFailures(KEY, AttemptLimiter.MAX_FAILURES - 1);
		
		assertDoesNotThrow(() -> limiter.checkAllowed(KEY));
	}
	
	@Test
	void checkAllowed_atLimit_throwsTooManyAttempts() {
		recordFailures(KEY, AttemptLimiter.MAX_FAILURES);
		
		TwoFactorException ex = assertThrows(TwoFactorException.class, () -> limiter.checkAllowed(KEY));
		
		assertEquals(ErrorCode.TOO_MANY_ATTEMPTS, ex.getCode());
		assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
	}
	
	@Test
	void checkAllowed_failuresOlderThanWindow_arePrunedAndAllowed() {
		recordFailures(KEY, AttemptLimiter.MAX_FAILURES);
		/// Precondition: blocked before time passes, so the final assert proves pruning unblocked it.
		assertThrows(TwoFactorException.class, () -> limiter.checkAllowed(KEY));
		
		clock.advance(AttemptLimiter.WINDOW.plusSeconds(1));
		
		assertDoesNotThrow(() -> limiter.checkAllowed(KEY));
	}
	
	@Test
	void checkAllowed_onlyOldFailuresArePruned_recentOnesStillCount() {
		/// 4 old failures that will expire, then 1 fresh one: total in-window = 1.
		recordFailures(KEY, AttemptLimiter.MAX_FAILURES - 1);
		clock.advance(AttemptLimiter.WINDOW.plusSeconds(1));
		limiter.recordFailure(KEY);
		
		assertDoesNotThrow(() -> limiter.checkAllowed(KEY));
		
		/// Topping up to the limit inside the new window blocks again.
		recordFailures(KEY, AttemptLimiter.MAX_FAILURES - 1);
		
		assertThrows(TwoFactorException.class, () -> limiter.checkAllowed(KEY));
	}
	
	@Test
	void checkAllowed_failuresSpreadAcrossWindow_slidingWindowApplies() {
		/// One failure per minute at minutes 0..4: at minute 4 all 5 are inside the 5-minute window.
		for (int i = 0; i < AttemptLimiter.MAX_FAILURES; i++) {
			if (i > 0) {
				clock.advance(Duration.ofMinutes(1));
			}
			limiter.recordFailure(KEY);
		}
		
		assertThrows(TwoFactorException.class, () -> limiter.checkAllowed(KEY));
		
		/// At 5:01 the minute-0 failure has slid out of the window, leaving 4.
		clock.advance(Duration.ofMinutes(1).plusSeconds(1));
		
		assertDoesNotThrow(() -> limiter.checkAllowed(KEY));
	}
	
	@Test
	@Disabled("Not written yet: decide whether a failure exactly WINDOW old still counts, then remove this")
	void checkAllowed_failureExactlyAtWindowEdge() {
		recordFailures(KEY, AttemptLimiter.MAX_FAILURES);
		
		clock.advance(AttemptLimiter.WINDOW);
		
		assertThrows(TwoFactorException.class, () -> limiter.checkAllowed(KEY));
	}
	
	@Test
	void reset_clearsFailuresForKey() {
		recordFailures(KEY, AttemptLimiter.MAX_FAILURES);
		/// Precondition: blocked before reset, so the final assert proves reset unblocked it.
		assertThrows(TwoFactorException.class, () -> limiter.checkAllowed(KEY));
		
		limiter.reset(KEY);
		
		assertDoesNotThrow(() -> limiter.checkAllowed(KEY));
	}
	
	@Test
	void reset_unknownKey_doesNotThrow() {
		assertDoesNotThrow(() -> limiter.reset(KEY));
	}
	
	@Test
	void keysAreIndependent() {
		/// OTHER_KEY has its own failures, so a shared count (9) would wrongly block it.
		recordFailures(KEY, AttemptLimiter.MAX_FAILURES);
		recordFailures(OTHER_KEY, AttemptLimiter.MAX_FAILURES - 1);
		
		assertThrows(TwoFactorException.class, () -> limiter.checkAllowed(KEY));
		assertDoesNotThrow(() -> limiter.checkAllowed(OTHER_KEY));
	}
	
	@Test
	void checkAllowed_doesNotCountAsFailure() {
		recordFailures(KEY, AttemptLimiter.MAX_FAILURES - 1);
		
		for (int i = 0; i < 10; i++) {
			limiter.checkAllowed(KEY);
		}
		
		assertDoesNotThrow(() -> limiter.checkAllowed(KEY));
	}
	
	@Test
	void reset_onlyAffectsGivenKey() {
		recordFailures(KEY, AttemptLimiter.MAX_FAILURES);
		recordFailures(OTHER_KEY, AttemptLimiter.MAX_FAILURES);
		
		limiter.reset(KEY);
		
		assertDoesNotThrow(() -> limiter.checkAllowed(KEY));
		assertThrows(TwoFactorException.class, () -> limiter.checkAllowed(OTHER_KEY));
	}
	
	// --- helpers ---
	
	/**
	 * Records {@code count} failures for {@code key} at the clock's current instant.
	 */
	private void recordFailures(String key, int count) {
		for (int i = 0; i < count; i++) {
			limiter.recordFailure(key);
		}
	}
	
	/**
	 * A {@link Clock} whose time only changes when a test calls {@link #advance(Duration)}.
	 */
	private static final class MutableClock extends Clock {
		private Instant now;
		
		MutableClock(Instant start) {
			this.now = start;
		}
		
		/**
		 * Moves the clock by {@code duration} (negative values move it back).
		 */
		void advance(Duration duration) {
			now = now.plus(duration);
		}
		
		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}
		
		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}
		
		@Override
		public Instant instant() {
			return now;
		}
	}
}
