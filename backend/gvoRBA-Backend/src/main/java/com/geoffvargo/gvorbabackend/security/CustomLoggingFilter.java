package com.geoffvargo.gvorbabackend.security;

import org.slf4j.*;
import org.springframework.stereotype.*;
import org.springframework.web.filter.*;

import java.io.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

@Component
public class CustomLoggingFilter extends OncePerRequestFilter {
	private static final Logger logger = LoggerFactory.getLogger(CustomLoggingFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
	throws ServletException, IOException {
		logger.info("CustomLoggingFilter --- Request URI: {}", request.getRequestURI());
		try {
			filterChain.doFilter(request, response);
		} finally {
			logger.info("CustomLoggingFilter --- Response Status: {}", response.getStatus());
		}
	}
}
