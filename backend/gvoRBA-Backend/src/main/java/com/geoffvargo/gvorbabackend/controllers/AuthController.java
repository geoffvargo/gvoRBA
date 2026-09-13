package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.models.DTOs.*;
import com.geoffvargo.gvorbabackend.models.User;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.security.jwt.*;
import com.geoffvargo.gvorbabackend.services.*;
import com.geoffvargo.gvorbabackend.services.RefreshTokenService.*;
import com.geoffvargo.gvorbabackend.utils.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.annotation.*;
import org.springframework.security.core.context.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.*;
import org.springframework.web.bind.annotation.*;

import java.security.*;
import java.time.*;
import java.util.*;
import java.util.logging.*;

import jakarta.validation.*;
import lombok.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
	public static final Logger LOGGER = Logger.getLogger(AuthController.class.getName());
	
	private final JwtUtils jwtUtils;
	
	private final AuthenticationManager authManager;
	
	private final UserRepository userRepository;
	
	private final RoleRepository roleRepository;
	
	private final UserDetailsService userDetailsService;
	
	private final PasswordEncoder passwordEncoder;
	
	private final UserService userService;
	
	private final AuthUtil authUtil;
	
	private final RefreshTokenService refreshTokenService;
	
	@Value("${spring.app.jwtExpirationMs}")
	private int expirationMs;
	
	private ResponseCookie buildRefreshCookie(String rawToken) {
		ResponseCookie ans = ResponseCookie.from("refreshToken")
			                     .value(rawToken)
			                     .path("/api/auth")
			                     .httpOnly(true)
			                     .secure(true)
			                     .sameSite("None")
			                     .maxAge(expirationMs / 60)
			                     .build();
		
		LOGGER.info(ans.toString());
		
		return ans;
	}
	
	private ResponseCookie clearRefreshToken(String rawToken) {
		ResponseCookie ans = ResponseCookie.from("refreshToken", "")
			                     .httpOnly(true)
			                     .secure(true)
			                     .sameSite("None")
			                     .path("/api/auth")
			                     .maxAge(Duration.ZERO)
			                     .build();
		
		LOGGER.info(ans.toString());
		
		return ans;
	}
	
	@PostMapping("/public/signin")
	public ResponseEntity<?> signin(@RequestBody LoginRequest loginRequest) {
		LOGGER.info("POST /public/signin starting...");
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
		
		/// get RefreshToken
		User user = userRepository.findByName(userDetails.getUsername()).orElseThrow(
			() -> new UsernameNotFoundException("User not found!")
		);
		String raw;
		try {
			raw = refreshTokenService.issue(user);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		
		/// Collect Roles from UserDetails
		List<String> roles = userDetails.getAuthorities().stream()
			                     .map(GrantedAuthority::getAuthority)
			                     .toList();
		
		/// prepare the response body
		LoginResponse response = new LoginResponse(jwtToken, userDetails.getUsername(), roles);
		
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
			buildRefreshCookie(raw).toString()).body(response);
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
			            .enabled(true)
			            .build();
		
		userRepository.save(user);
		
		return ResponseEntity.ok("User registered successfully!");
	}
	
	@PostMapping("/public/refresh")
	public ResponseEntity<?> refresh(@CookieValue(name = "refreshToken", required = false) String cookieValue) {
		if (cookieValue == null) {
			throw new RuntimeException("Null cookie name.");
		}
		
		RotationResult res = refreshTokenService.rotate(cookieValue);
		String jwt = jwtUtils.generateTokenFromUsername(res.principal());
		
		List<String> roles = res.principal().getAuthorities().stream()
			                     .map(GrantedAuthority::getAuthority)
			                     .toList();
		
		return ResponseEntity.ok(new LoginResponse(jwt, res.principal().getUsername(), roles).toString() +
		                         buildRefreshCookie(res.rawToken()));
	}
	
	@PostMapping("/public/logout")
	public ResponseEntity<?> logout(@CookieValue(name = "refreshToken", required = false) String cookieValue) {
		if (cookieValue != null) {
			refreshTokenService.revoke(cookieValue);
		}
		
		return ResponseEntity.ok(clearRefreshToken(cookieValue));
	}
	
	@GetMapping("/getUser")
	public ResponseEntity<UserDto> getUser(@AuthenticationPrincipal UserDetails userDetails) {
		User user = userRepository.findByName(userDetails.getUsername()).orElseThrow(
			() -> new UsernameNotFoundException("User not found!")
		);
		
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
	
	@GetMapping("/me")
	public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
		User user = userRepository.findByName(userDetails.getUsername()).orElseThrow();
		
		return ResponseEntity.ok(UserDto.fromUser(user));
	}
}
