package com.geoffvargo.gvorbabackend.repos;

import com.geoffvargo.gvorbabackend.models.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.*;
import org.springframework.stereotype.*;

import java.util.*;

@Repository
public interface UserPrefsRepository extends JpaRepository<UserPrefs, Long> {
	UserPrefs findByUserId(Long id);
	
	@Query("""
	       SELECT pref.user FROM UserPrefs pref JOIN pref.favoriteRooms room
	       WHERE room = :roomId
	       	       AND pref.emailNotification = TRUE
	       	       AND pref.notifyOnRoomDeactivated = TRUE
	       """)
	List<User> findRoomDeactivationRecipients(@Param("roomId") Long roomId);
}
