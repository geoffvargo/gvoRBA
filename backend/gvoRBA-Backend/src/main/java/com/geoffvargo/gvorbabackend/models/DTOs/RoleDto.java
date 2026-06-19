package com.geoffvargo.gvorbabackend.models.DTOs;

import com.geoffvargo.gvorbabackend.models.*;

import java.io.*;

/**
 * DTO for {@link com.geoffvargo.gvorbabackend.models.Role}
 */
public record RoleDto(Long id, AppRole roleName) implements Serializable {
	public static RoleDto fromRole(Role role) {
		return new RoleDto(
			role.getId(),
			role.getRoleName()
		);
	}
}
