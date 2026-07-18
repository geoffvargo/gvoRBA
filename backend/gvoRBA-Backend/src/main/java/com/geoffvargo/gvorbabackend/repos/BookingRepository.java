package com.geoffvargo.gvorbabackend.repos;

import com.geoffvargo.gvorbabackend.models.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.*;
import org.springframework.stereotype.*;

import java.time.*;
import java.util.*;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
	Optional<Booking> findByRoom_Id(Long id);
	Optional<Booking> findByCancelledAt(LocalDateTime cancelledAt);
	Optional<Booking> findByUserId(Long id);
	List<Booking> findAllByUserId(Long id);
	List<Booking> findAllByRoom_Id(Long id);
	
	@Query("""
		SELECT b FROM Booking b
		WHERE b.room.id = :roomId
		  AND b.cancelledAt IS NULL
		  AND b.startsAt < :endExclusive
		  AND b.endsAt   > :startInclusive
		ORDER BY b.startsAt
		""")
	List<Booking> findForRoomInRange(@Param("roomId") Long roomId,
	                                 @Param("startInclusive") LocalDateTime startInclusive,
	                                 @Param("endExclusive") LocalDateTime endExclusive);
	
	// "optional" third parameter — omit it and get a single day
	default List<Booking> findForRoomInRange(Long roomId, LocalDateTime startInclusive) {
		return findForRoomInRange(roomId, startInclusive, startInclusive.plusDays(1));
	}
}
