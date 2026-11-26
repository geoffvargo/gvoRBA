package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.models.DTOs.*;
import com.geoffvargo.gvorbabackend.repos.*;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import lombok.*;
import lombok.extern.slf4j.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/directory")
public class UserDirectoryController {
	private final UserRepository userRepository;
	
	@GetMapping("/users")
	public ResponseEntity<List<UserSummaryDto>> listUsers() {
		List<User> users = userRepository.findAllByEnabledTrue();
		
		val ans = users.stream().map(UserSummaryDto::fromUser).toList();
		
		return ResponseEntity.ok(ans);
	}
}
