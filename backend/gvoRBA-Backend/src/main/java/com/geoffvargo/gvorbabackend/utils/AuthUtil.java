package com.geoffvargo.gvorbabackend.utils;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.stereotype.*;

import java.util.*;

@Component
public class AuthUtil {
	@Autowired
	private UserRepository userRepository;
	
	public Long loggedInUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		
		Optional<User> user = Optional.empty();
		if (auth != null) {
			user = userRepository.findByName(auth.getName().describeConstable().orElseThrow(
				() -> new RuntimeException("Username not found!")));
		}
		return user.orElseThrow().getId();
	}
	
	public User getLoggedInUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return userRepository.findByName(Objects.requireNonNull(auth).getName()).orElseThrow(
			() -> new RuntimeException("Username not found!")
		);
	}
}
