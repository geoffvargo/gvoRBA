package com.geoffvargo.gvorbabackend.security.jwt;

import org.jspecify.annotations.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.authentication.*;
import org.springframework.stereotype.*;
import org.springframework.web.filter.*;

import java.io.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
	public static final Logger LOGGER = LoggerFactory.getLogger(AuthTokenFilter.class);
	
	@Autowired
	private JwtUtils jwtUtils;
	
	@Autowired
	private UserDetailsService userDetailsService;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
	                                @NonNull FilterChain filterChain)
	throws ServletException, IOException {
		LOGGER.debug("AuthTokenFilter called for URI: {}", request.getRequestURI());
		
		try {
			String jwt = parseJwt(request);
			
			if (jwt != null && jwtUtils.validateToken(jwt)) {
				String username = jwtUtils.getUsernameFromToken(jwt);
				
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					userDetails, null, userDetails.getAuthorities()
				);
				
				LOGGER.debug("Roles from JWT: {}", userDetails.getAuthorities());
				
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		
		filterChain.doFilter(request, response);
	}
	
	private String parseJwt(HttpServletRequest request) {
		String jwt = jwtUtils.getJwtFromHeader(request);
		
		LOGGER.debug("AuthTokenFilter.java: {}", jwt);
		
		return jwt;
	}
}
