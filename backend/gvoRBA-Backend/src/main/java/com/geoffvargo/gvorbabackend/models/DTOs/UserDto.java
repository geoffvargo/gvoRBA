package com.geoffvargo.gvorbabackend.models.DTOs;

import com.fasterxml.jackson.annotation.*;
import com.geoffvargo.gvorbabackend.models.*;

import java.io.*;
import java.util.*;

/**
 * DTO for {@link User}
 */
public record UserDto(
	Long id,
	String email,
	String name,
	RoleDto role,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	Date createdOn,
	Boolean enabled,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	String password,
	AuthProvider authProvider) implements Serializable {
	public UserDto(Long id,
	               String email,
	               String name,
	               RoleDto role,
	               Date createdOn,
	               Boolean enabled,
	               String authProvider) {
		this(id, email, name, role, createdOn, enabled, null, AuthProvider.valueOf(authProvider));
	}
	
	public UserDto(Long id,
	               String email,
	               String name,
	               RoleDto role,
	               Boolean enabled,
	               String password) {
		this(id, email, name, role, null, enabled, password);
	}
	
	public static UserDto fromUser(User user) {
		return new UserDto(
			user.getId(),
			user.getEmail(),
			user.getName(),
			RoleDto.fromRole(user.getRole()),
			user.getCreatedOn(),
			user.getEnabled(),
			user.getAuthProvider().toString()
		);
	}
}
