package com.geoffvargo.gvorbabackend.security;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.models.Role;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.security.jwt.*;
import com.geoffvargo.gvorbabackend.security.oauth2.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
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

import lombok.*;

/**
 * Configures Spring Security for the application.
 *
 * <p>Sets up stateless JWT authentication for the API, OAuth2 login through Google and GitHub,
 * CORS for the Angular frontend, and seed data on startup.
 */
@Configuration
@EnableWebSecurity
@EnableScheduling /// enables @Scheduled jobs elsewhere in the app
@RequiredArgsConstructor
public class SecurityConfig {
	private final AuthEntryPointJwt unauthorizedHandler;
	
	private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
	
	@Value("${app.cors.allowed-origins:http://localhost:4200}")
	private String allowedOrigins;
	
	/**
	 * Creates the password encoder used to hash passwords on signup and verify them on login.
	 *
	 * @return a BCrypt-based {@link PasswordEncoder}
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	/**
	 * Exposes Spring's {@link AuthenticationManager} as a bean.
	 *
	 * <p>{@code AuthController} uses it to authenticate username and password logins.
	 *
	 * @param authConfig
	 * 	Spring's authentication configuration
	 * @return the application's {@link AuthenticationManager}
	 * @throws Exception
	 * 	if the manager cannot be built
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}
	
	/**
	 * Builds the main security filter chain.
	 *
	 * <p>Defines which endpoints are public, enables OAuth2 login, and registers the logging and
	 * JWT filters.
	 *
	 * @param http
	 * 	the {@link HttpSecurity} builder to configure
	 * @param customLoggingFilter
	 * 	the filter that logs each incoming request
	 * @return the configured {@link SecurityFilterChain}
	 * @throws Exception
	 * 	if the chain cannot be built
	 */
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, CustomLoggingFilter customLoggingFilter)
	throws Exception {
		/// CSRF is off because API auth uses the Authorization: Bearer header, not session cookies.
		http.csrf(AbstractHttpConfigurer::disable);
		http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
		
		/// Public endpoints are listed explicitly; everything else requires a valid JWT.
		/// /oauth2/** and /login/oauth2/** are Spring's OAuth2 authorize and callback URLs.
		http.authorizeHttpRequests(requests ->
			                           requests
				                           .requestMatchers("/api/health").permitAll()
				                           .requestMatchers("/api/ping").permitAll()
				                           .requestMatchers("/api/auth/public/**").permitAll()
				                           .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
				                           .requestMatchers("/api/dev/**").permitAll()
				                           .anyRequest().authenticated());
		http.oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler));
		http.exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler));
		
		/// Both filters run before Spring's form-login filter: log the request, then read the JWT
		/// and populate the SecurityContext.
		http.addFilterBefore(customLoggingFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(authTokenFilter(), UsernamePasswordAuthenticationFilter.class);
		
		return http.build();
	}
	
	/**
	 * Creates the CORS configuration for all API paths.
	 *
	 * <p>Lets the configured frontend origins call the API with credentials (cookies). Because
	 * {@code allowCredentials} is {@code true}, origins must be listed explicitly; {@code "*"} is
	 * not allowed.
	 *
	 * @return the {@link CorsConfigurationSource} applied to {@code /**}
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
			                         .map(String::trim)
			                         .toList());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
	
	/**
	 * Creates the filter that validates the JWT on each request.
	 *
	 * @return an {@link AuthTokenFilter} that sets the authenticated user when the token is valid
	 */
	@Bean
	public AuthTokenFilter authTokenFilter() {
		return new AuthTokenFilter();
	}
	
	/**
	 * Seeds the database with roles, test accounts, and sample rooms on startup.
	 *
	 * <p>Creates the {@code user1} and {@code admin} accounts (password {@code password1}). Each
	 * step checks for existing data first, so restarts don't create duplicates.
	 *
	 * @param userRepository
	 * 	repository for users
	 * @param roleRepository
	 * 	repository for roles
	 * @param passwordEncoder
	 * 	encoder used to hash the seed passwords
	 * @param roomRepository
	 * 	repository for rooms
	 * @return a {@link CommandLineRunner} that inserts the seed data
	 */
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
				user1.setEnabled(true);
				
				userRepository.save(user1);
			}
			
			if (!userRepository.existsByName("admin")) {
				User user2 = new User();
				user2.setName("admin");
				user2.setEmail("admin@blah.com");
				user2.setPassword(passwordEncoder.encode("password1"));
				user2.setRole(adminRole);
				user2.setCreatedOn(new Date());
				user2.setEnabled(true);
				userRepository.save(user2);
			}
			
			if (roomRepository.findAll().isEmpty()) {
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
			}
		};
	}
}
