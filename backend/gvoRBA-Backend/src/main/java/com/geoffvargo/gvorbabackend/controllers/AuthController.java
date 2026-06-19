package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.models.DTOs.*;
import com.geoffvargo.gvorbabackend.models.User;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.security.jwt.*;
import com.geoffvargo.gvorbabackend.services.*;
import com.geoffvargo.gvorbabackend.utils.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.annotation.*;
import org.springframework.security.core.context.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import jakarta.validation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {
	@Autowired
	private JwtUtils jwtUtils;
	
	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private RoleRepository roleRepository;
	
	@Autowired
	private UserDetailsService userDetailsService;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private AuthUtil authUtil;
	
	@PostMapping("/public/signin")
	public ResponseEntity<?> signin(@RequestBody LoginRequest loginRequest) {
		Authentication auth;
		
		try {
			auth = authManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
		} catch (AuthenticationException e) {
			Map<String, String> map = new HashMap<>();
			map.put("message", "Invalid username or password!");
			map.put("error", e.getMessage());
			return new ResponseEntity<>(map, HttpStatus.NOT_FOUND);
		}
		
		/// setting authentication
		SecurityContextHolder.getContext().setAuthentication(auth);
		
		UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
		String jwtToken = jwtUtils.generateTokenFromUsername(Objects.requireNonNull(userDetails));
		
		/// Collect Roles from UserDetails
		List<String> roles = userDetails.getAuthorities().stream()
			                     .map(GrantedAuthority::getAuthority)
			                     .toList();
		
		/// prepare the response body
		LoginResponse response = new LoginResponse(jwtToken, userDetails.getUsername(), roles);
		
		return ResponseEntity.ok(response);
	}
	
	@PostMapping("/public/signup")
	public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
		if (userRepository.existsByName(request.getUsername())) {
			return ResponseEntity.badRequest().body("Username is already in use!");
		}
		
		if (userRepository.existsByEmail(request.getEmail())) {
			return ResponseEntity.badRequest().body("Email is already in use!");
		}
		
		/// Create the new user's account
		User user = User.builder()
			            .name(request.getUsername())
			            .email(request.getEmail())
			            .role(parseRole())
			            .password(passwordEncoder.encode(request.getPassword()))
			            .createdOn(new Date())
			            .build();
		
		userRepository.save(user);
		
		return ResponseEntity.ok("User registered successfully!");
	}
	
	@GetMapping("/getUser")
	public ResponseEntity<UserDto> getUser(@AuthenticationPrincipal UserDetails userDetails) {
		User user = userRepository.findByName(userDetails.getUsername()).orElseThrow(
			() -> new UsernameNotFoundException("User not found!")
		);

//		List<String> roles = userDetails.getAuthorities().stream()
//			                     .map(GrantedAuthority::getAuthority)
//			                     .toList();
		
		return ResponseEntity.ok(UserDto.fromUser(user));
	}
	
	private Role parseRole() {
		if ("ROLE_USER".matches("ROLE_ADMIN")) {
			return roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow(
				() -> new RuntimeException("Role not found!"));
		} else {
			return roleRepository.findByRoleName(AppRole.valueOf("ROLE_USER")).orElseThrow(
				() -> new RuntimeException("Role not found!"));
		}
	}
}
