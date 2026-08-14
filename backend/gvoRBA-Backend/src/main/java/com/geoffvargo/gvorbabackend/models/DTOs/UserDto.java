package com.geoffvargo.gvorbabackend.models.DTOs;

import com.fasterxml.jackson.annotation.*;
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
	@JsonInclude(JsonInclude.Include.NON_NULL) Date createdOn,
	Boolean enabled,
	@JsonInclude(JsonInclude.Include.NON_NULL) String password

) implements Serializable {
	public UserDto(String email,
	               String name,
	               RoleDto role,
	               Date createdOn,
	               Boolean enabled) {
		this(email, name, role, createdOn, enabled, null);
	}
	
	public UserDto(String email,
	               String name,
	               RoleDto role,
	               Boolean enabled,
	               String password) {
		this(email, name, role, null, enabled, password);
	}
	
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
