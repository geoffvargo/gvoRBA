package com.geoffvargo.gvorbabackend.models;

import java.util.*;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
public class SignupRequest {
	@NotBlank
	private String username;
	
	@NotBlank
	private String email;
	
	@NotBlank
	private String password;
}
