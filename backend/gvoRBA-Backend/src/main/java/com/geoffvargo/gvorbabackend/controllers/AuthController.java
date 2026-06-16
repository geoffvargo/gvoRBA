package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.security.jwt.*;
import com.geoffvargo.gvorbabackend.services.*;
import com.geoffvargo.gvorbabackend.utils.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {
	@Autowired
	private JwtUtils jwtUtils;
	
	@Autowired
	private AuthenticationManager authManager;
	
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
}
