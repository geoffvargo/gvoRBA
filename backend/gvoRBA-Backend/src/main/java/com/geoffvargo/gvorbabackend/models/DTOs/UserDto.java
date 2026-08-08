package com.geoffvargo.gvorbabackend.models.DTOs;

import com.geoffvargo.gvorbabackend.models.*;

import java.io.*;
import java.util.*;

/**
 * DTO for {@link User}
 */
public record UserDto(
	String email,
	String name,
	RoleDto role,
	Date createdOn,
	Boolean enabled

) implements Serializable {
	public static UserDto fromUser(User user) {
		return new UserDto(
			user.getEmail(),
			user.getName(),
			RoleDto.fromRole(user.getRole()),
			user.getCreatedOn(),
			user.getEnabled()
		);
	}
}
