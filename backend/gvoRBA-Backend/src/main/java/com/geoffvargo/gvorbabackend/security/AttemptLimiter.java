package com.geoffvargo.gvorbabackend.security;

import com.geoffvargo.gvorbabackend.exceptions.*;

import org.springframework.http.*;
import org.springframework.stereotype.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import lombok.*;
import lombok.extern.slf4j.*;

/**
 * Limits failed attempts per key (such as a username) using a sliding time window.
 *
 * <p>A key is blocked once it has {@link #MAX_FAILURES} failures within the last {@link #WINDOW}.
 * A 6-digit code has 10^6 values and about 3 are valid at any moment (drift window), so without a
 * limit an attacker who already has the first factor could brute-force the second one quickly.
 *
 * <p>State is kept in memory, which is fine for one instance. With several instances, back this
 * with Redis or a DB table.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttemptLimiter {
	/**
	 * Number of failures within {@link #WINDOW} that blocks a key.
	 */
	public static final int MAX_FAILURES = 5;

	/**
	 * How far back a failure still counts toward {@link #MAX_FAILURES}.
	 */
	public static final Duration WINDOW = Duration.ofMinutes(5);

	/**
	 * Failure timestamps per key, oldest first. Each deque is guarded by its own lock.
	 */
	private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

	/**
	 * Source of the current time, injected so tests can control it.
	 */
	private final Clock clock;

	/**
	 * Throws if {@code key} has reached the failure limit within the current window.
	 *
	 * <p>Expired failures are removed first, so a key unblocks once enough of its failures are
	 * older than {@link #WINDOW}. Calling this method does not count as a failure.
	 *
	 * @param key
	 * 	the key to check, such as a username
	 * @throws TwoFactorException
	 * 	with {@link ErrorCode#TOO_MANY_ATTEMPTS} and HTTP 429 if {@code key} has
	 * 	{@link #MAX_FAILURES} or more failures within {@link #WINDOW}
	 */
	public void checkAllowed(String key) {
		Deque<Instant> attempts = failures.get(key);

		if (attempts == null || attempts.isEmpty()) {
			return;
		}

		synchronized (attempts) {
			prune(attempts);
			if (attempts.size() >= MAX_FAILURES) {
				throw new TwoFactorException(ErrorCode.TOO_MANY_ATTEMPTS, HttpStatus.TOO_MANY_REQUESTS);
			}
		}
	}

	/**
	 * Removes failures older than {@link #WINDOW} from {@code attempts}.
	 *
	 * <p>A failure exactly {@link #WINDOW} old is kept. The caller must hold the lock on
	 * {@code attempts}.
	 *
	 * @param attempts
	 * 	the failure timestamps for one key
	 */
	private void prune(Deque<Instant> attempts) {
		Instant instant = clock.instant().minus(WINDOW);

		attempts.removeIf(i -> i.isBefore(instant));
	}

	/**
	 * Records a failed attempt for {@code key} at the current time.
	 *
	 * @param key
	 * 	the key the failure belongs to, such as a username
	 */
	public void recordFailure(String key) {
		Deque<Instant> attempts = failures.computeIfAbsent(key, k -> new ArrayDeque<>());

		synchronized (attempts) {
			attempts.addLast(clock.instant());
		}
	}

	/**
	 * Clears all recorded failures for {@code key}, typically after a successful attempt.
	 *
	 * <p>Does nothing if {@code key} has no recorded failures.
	 *
	 * @param key
	 * 	the key to clear
	 */
	public void reset(String key) {
		failures.remove(key);
	}
}