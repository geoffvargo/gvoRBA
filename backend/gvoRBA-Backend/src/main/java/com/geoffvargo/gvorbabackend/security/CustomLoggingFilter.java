package com.geoffvargo.gvorbabackend.security;

import org.springframework.stereotype.*;
import org.springframework.web.filter.*;

import java.io.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

@Component
public class CustomLoggingFilter extends OncePerRequestFilter {
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
	throws ServletException, IOException {
		System.out.println("CustomLoggingFilter --- Request URI: " + request.getRequestURI());
		filterChain.doFilter(request, response);
		System.out.println("CustomLoggingFilter --- Response Status: " + response.getStatus());
	}
}
