package com.geoffvargo.gvorbabackend.repos;

import com.geoffvargo.gvorbabackend.models.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.*;
import org.springframework.stereotype.*;

import java.time.*;

@Repository
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {
	/**
	 * Atomically marks one of a user's recovery codes as used.
	 *
	 * <p>The check and the write happen in a single {@code UPDATE}: the row changes only when the code
	 * belongs to this user and {@code usedAt} is still {@code null}. Two concurrent requests that submit
	 * the same code therefore cannot both succeed; a read-then-mark-used check would let both through.
	 *
	 * <p>{@code clearAutomatically = true} detaches every entity in the current persistence context.
	 * Callers must re-load the {@link com.geoffvargo.gvorbabackend.models.User} before changing it.
	 *
	 * @param userId   the owner's primary key
	 * @param codeHash SHA-256 hex of the normalized recovery code
	 * @param now      the time to record in {@code usedAt}
	 * @return {@code 1} if this call used the code; {@code 0} if the code is unknown or already used
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
	       UPDATE RecoveryCode r
	          SET r.usedAt = :now
	        WHERE r.user.id = :userId
	          AND r.codeHash = :codeHash
	          AND r.usedAt IS NULL
	       """)
	int claim(@Param("userId") Long userId, @Param("codeHash") String codeHash, @Param("now") LocalDateTime now);
	
	/**
	 * Deletes all of a user's recovery codes in one bulk {@code DELETE}, without loading them first.
	 *
	 * @param userId the owner's primary key
	 * @return the number of codes deleted
	 */
	@Modifying
	@Query("""
	       DELETE FROM RecoveryCode r
	       			WHERE r.user.id = :userId
	       """)
	int deleteAllForUser(@Param("userId") Long userId);
	
	
}
