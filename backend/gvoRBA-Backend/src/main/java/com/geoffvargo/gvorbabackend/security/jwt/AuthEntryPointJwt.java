package com.geoffvargo.gvorbabackend.security.jwt;

import org.slf4j.*;
import org.springframework.security.core.*;
import org.springframework.security.web.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.util.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import tools.jackson.databind.*;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
	public static final Logger LOGGER = LoggerFactory.getLogger(AuthEntryPointJwt.class);
	
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
	                     AuthenticationException authException) throws IOException, ServletException {
		LOGGER.error("Unauthorized access");
		
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		
		final Map<String, Object> map = new HashMap<>();
		
		map.put("atatus", HttpServletResponse.SC_UNAUTHORIZED);
		map.put("error", "Unauthorized access");
		map.put("message", authException.getMessage());
		map.put("path", request.getServletPath());
		
		final ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(response.getOutputStream(), map);
	}
}
