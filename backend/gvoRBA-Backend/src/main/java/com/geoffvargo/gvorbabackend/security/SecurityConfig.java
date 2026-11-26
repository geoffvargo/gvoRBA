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
import org.springframework.security.authorization.*;
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
	private final TotpFactorAuthorizationManager factorAuthManager;
	
	private final MfaAccessDeniedHandler mfaAccessDeniedHandler;
	
	private final AuthEntryPointJwt unauthorizedHandler;
	
	private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
	
	private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
	
	private final CustomOAuth2UserService customOAuth2UserService;
	
	private final CustomOidcUserService customOidcUserService;
	
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
		DefaultAuthorizationManagerFactory<Object> mfa = new DefaultAuthorizationManagerFactory<>();
		
		mfa.setAdditionalAuthorization(factorAuthManager);
		
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
				                           
				                           /// Without this, any uncaught exception is re-dispatched to /error,
				                           /// rejected here, and surfaces as a misleading 401.
				                           .requestMatchers("/error").permitAll()
				                           
				                           /// This is to allow mutlifactor/2fa to work
				                           .requestMatchers(
					                           "/api/auth/2fa/verify",
					                           "/api/auth/2fa/recover"
				                           ).authenticated()
				                           .anyRequest().access(mfa.authenticated()));
		
		/// The user services provision the local account (with ROLE_USER) during login:
		/// the OIDC one handles Google (openid scope), the plain OAuth2 one handles GitHub.
		http.oauth2Login(oauth2 -> oauth2
			                           .userInfoEndpoint(userInfo -> userInfo
				                                                         .userService(customOAuth2UserService)
				                                                         .oidcUserService(customOidcUserService))
			                           .successHandler(oAuth2LoginSuccessHandler)
			                           .failureHandler(oAuth2LoginFailureHandler));
		
		/// This is for exception handling
		http.exceptionHandling(ex -> {
				ex.authenticationEntryPoint(unauthorizedHandler);
				ex.accessDeniedHandler(mfaAccessDeniedHandler);
			}
		);
		
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
	
}
