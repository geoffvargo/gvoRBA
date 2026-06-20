package com.geoffvargo.gvorbabackend.security;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.models.Role;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.security.jwt.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.*;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.config.annotation.web.configurers.*;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.*;
import org.springframework.web.cors.*;

import java.time.*;
import java.util.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	@Autowired
	private AuthEntryPointJwt unauthorizedHandler;
	
	@Value("${app.cors.allowed-origins:http://localhost:4200}")
	private String allowedOrigins;
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}
	
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, CustomLoggingFilter customLoggingFilter)
	throws Exception {
		http.csrf(AbstractHttpConfigurer::disable);
		http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
		http.authorizeHttpRequests(requests ->
			                           requests
				                           .requestMatchers("/api/health").permitAll()
				                           .requestMatchers("/api/ping").permitAll()
				                           .requestMatchers("/api/auth/public/**").permitAll()
				                           .anyRequest().authenticated());
		http.exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler));
		http.addFilterBefore(customLoggingFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(authTokenFilter(), UsernamePasswordAuthenticationFilter.class);
		
		return http.build();
	}
	
	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
	
	@Bean
	public AuthTokenFilter authTokenFilter() {
		return new AuthTokenFilter();
	}
	
	@Bean
	public CommandLineRunner initData(UserRepository userRepository, RoleRepository roleRepository,
	                                  PasswordEncoder passwordEncoder, RoomRepository roomRepository) {
		return args -> {
			Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
				                .orElseGet(() ->
					                           roleRepository.save(new Role(AppRole.ROLE_USER)));
			
			Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
				                 .orElseGet(() ->
					                            roleRepository.save(new Role(AppRole.ROLE_ADMIN)));
			
			if (!userRepository.existsByName("user1")) {
				User user1 = new User();
				user1.setName("user1");
				user1.setEmail("user1@blah.com");
				user1.setPassword(passwordEncoder.encode("password1"));
				user1.setRole(userRole);
				user1.setCreatedOn(new Date());
				
				userRepository.save(user1);
			}
			
			if (!userRepository.existsByName("admin")) {
				User user2 = new User();
				user2.setName("admin");
				user2.setEmail("admin@blah.com");
				user2.setPassword(passwordEncoder.encode("password1"));
				user2.setRole(adminRole);
				user2.setCreatedOn(new Date());
				userRepository.save(user2);
			}
			
			roomRepository.save(new Room(
				"Cedar Conference Room",
				"Building A, Floor 2",
				12,
				List.of("Projector", "Whiteboard", "Video Conferencing", "WiFi"),
				true,
				LocalDate.of(2024, 3, 15).atStartOfDay()
			));
			roomRepository.save(new Room(
				"Summit Boardroom",
				"Building B, Floor 5",
				20,
				List.of("4K Display", "Surround Sound", "Climate Control", "WiFi", "Coffee Station"),
				true,
				LocalDate.of(2024, 6, 1).atStartOfDay()
			));
			roomRepository.save(new Room(
				"Birch Huddle Space",
				"Building A, Floor 1",
				4,
				List.of("Whiteboard", "WiFi"),
				false,
				LocalDate.of(2023, 11, 20).atStartOfDay()
			));
			roomRepository.save(new Room(
				"Horizon Training Room",
				"Building C, Floor 3",
				30,
				List.of("Projector", "Microphone", "Recording Equipment", "WiFi", "Whiteboard"),
				true,
				LocalDate.of(2024, 1, 8).atStartOfDay()
			));
			roomRepository.save(new Room(
				"Ember Focus Pod",
				"Building B, Floor 2",
				2,
				List.of("WiFi", "Noise Cancellation"),
				true,
				LocalDate.of(2024, 9, 30).atStartOfDay()
			));
		};
	}
}
