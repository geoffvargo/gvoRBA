package com.geoffvargo.gvorbabackend.models;

import java.util.*;

import lombok.*;

@Data
@AllArgsConstructor
public class LoginResponse {
	private String jwtToken;
	private String username;
	private List<String> roles;
}
