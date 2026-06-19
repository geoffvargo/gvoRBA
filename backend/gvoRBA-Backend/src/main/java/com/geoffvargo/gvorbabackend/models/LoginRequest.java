package com.geoffvargo.gvorbabackend.models;

import lombok.*;

@Data
public class LoginRequest {
	private String username;
	private String password;
}
