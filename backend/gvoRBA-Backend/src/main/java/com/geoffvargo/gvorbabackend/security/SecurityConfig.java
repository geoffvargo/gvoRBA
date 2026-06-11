package com.geoffvargo.gvorbabackend.security;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.web.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(requests ->
			                           requests
				                           .requestMatchers("/api/health").permitAll()
				                           .anyRequest().authenticated());
		
		return http.build();
	}
}
