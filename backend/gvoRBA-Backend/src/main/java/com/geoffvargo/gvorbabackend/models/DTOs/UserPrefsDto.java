package com.geoffvargo.gvorbabackend.models.DTOs;

import com.geoffvargo.gvorbabackend.models.*;

import java.io.*;
import java.util.*;

/**
 * DTO for {@link UserPrefs}
 */
public record UserPrefsDto(
	Boolean emailNotification,
	Boolean multifactor,
	Boolean confirmBookings,
	Boolean notifyOnAdminCancel,
	Boolean notifyOnBookingChange,
	Boolean notifyOnRoomDeactivated,
	Set<Long> favoriteRooms
) implements Serializable {
	
	/**
	 * Builds the DTO from a user's prefs, falling back to the defaults when the user has none.
	 *
	 * @param prefs
	 * 	the user's prefs, or {@code null}
	 * @return the prefs DTO
	 */
	public static UserPrefsDto fromUserPrefs(UserPrefs prefs) {
		if (prefs == null) {
			return new UserPrefsDto(false, false, false, false, false, false, Set.of());
		}
		
		return new UserPrefsDto(
			prefs.getEmailNotification(),
			prefs.getMultifactor(),
			prefs.getConfirmBookings(),
			prefs.getNotifyOnAdminCancel(),
			prefs.getNotifyOnBookingChange(),
			prefs.getNotifyOnRoomDeactivated(),
			Set.copyOf(prefs.getFavoriteRooms())
		);
	}
}
