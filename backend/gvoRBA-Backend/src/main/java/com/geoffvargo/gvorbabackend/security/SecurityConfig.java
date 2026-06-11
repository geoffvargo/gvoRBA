package com.geoffvargo.gvorbabackend.security;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.config.annotation.web.configurers.*;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable);
		http.authorizeHttpRequests(requests ->
			                           requests
				                           .requestMatchers("/api/health").permitAll()
				                           .requestMatchers("/api/ping").permitAll()
				                           .anyRequest().authenticated());
		http.addFilterBefore(new CustomLoggingFilter(), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
