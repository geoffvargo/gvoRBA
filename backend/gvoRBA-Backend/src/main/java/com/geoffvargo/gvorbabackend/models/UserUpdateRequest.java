package com.geoffvargo.gvorbabackend.models;

import com.geoffvargo.gvorbabackend.models.DTOs.*;

import lombok.*;

@Data
@AllArgsConstructor
public class UserUpdateRequest {
	String email;
	String name;
	Role role;
	Boolean enabled;
	
	public static UserUpdateRequest fromUser(User user) {
		return new UserUpdateRequest(
			user.getEmail(),
			user.getName(),
			user.getRole(),
			user.getEnabled()
		);
	}
}
