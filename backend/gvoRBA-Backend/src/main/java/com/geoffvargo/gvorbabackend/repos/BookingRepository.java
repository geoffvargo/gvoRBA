package com.geoffvargo.gvorbabackend.repos;

import com.geoffvargo.gvorbabackend.models.*;

import org.springframework.data.jpa.repository.*;
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
	
}
