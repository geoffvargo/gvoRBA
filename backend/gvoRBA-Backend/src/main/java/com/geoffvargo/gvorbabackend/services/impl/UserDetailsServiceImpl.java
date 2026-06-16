package com.geoffvargo.gvorbabackend.services.impl;

import com.geoffvargo.gvorbabackend.models.User;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.security.jwt.*;

import org.jspecify.annotations.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.*;

import jakarta.transaction.*;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	@Autowired
	private UserRepository userRepository;
	
	@Override
	@Transactional
	public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
		User user = userRepository.findByName(username).orElseThrow(
			() -> new UsernameNotFoundException("User not found: {}")
		);
		
		return UserDetailsImpl.build(user);
	}
}
