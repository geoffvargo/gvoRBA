package com.geoffvargo.gvorbabackend.models.DTOs;

import com.geoffvargo.gvorbabackend.models.*;

public record UserSummaryDto(
	Long id,
	String name
) {
	
	public static UserSummaryDto fromUser(User user) {
		return new UserSummaryDto(user.getId(), user.getName());
	}
}
