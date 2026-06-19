package com.geoffvargo.gvorbabackend.repos;

import com.geoffvargo.gvorbabackend.models.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;

import java.util.*;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
	
	Optional<Booking> findByRoomId_Id(Long id);
}
