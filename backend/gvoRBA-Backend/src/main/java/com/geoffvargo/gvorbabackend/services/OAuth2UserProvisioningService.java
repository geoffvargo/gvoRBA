package com.geoffvargo.gvorbabackend.services;

import com.geoffvargo.gvorbabackend.exceptions.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.security.oauth2.*;

import org.hibernate.*;
import org.springframework.stereotype.*;

import java.util.*;

import jakarta.transaction.*;
import lombok.*;

@Service
@RequiredArgsConstructor
public class OAuth2UserProvisioningService {
	private final UserRepository userRepository;
	
	private final RoleRepository roleRepository;
	
	@Transactional
	public User provision(AuthProvider provider, OAuth2UserInfo userInfo) {
		Optional<User> existing = userRepository.findByAuthProviderAndProviderId(provider, userInfo.getProviderId());
		User user;
		
		if (existing.isPresent()) {
			user = existing.get();
			if (!userInfo.getName().isBlank()) {
				user.setName(userInfo.getName());
			}
			forceRoleLoad(user);
			return user;
		}
		
		String email = userInfo.getEmail();
		
		if (email.isBlank()) {
			throw new OAuth2AuthenticationException("Blank Email.");
		} else if (userRepository.existsByEmail(email)) {
			throw new OAuth2AuthenticationException("Email already exists!");
		}
		
		Role role;
		Optional<Role> roleOpt = roleRepository.findByRoleName(AppRole.ROLE_USER);
		if (roleOpt.isEmpty()) {
			throw new OAuth2AuthenticationException("Roel is empty!");
		} else {
			role = roleOpt.get();
		}
		
		User newUser = User.builder()
			               .name(userInfo.getName())
			               .email(email)
			               .password(null)
			               .role(role)
			               .enabled(true)
			               .createdOn(new Date())
			               .authProvider(provider)
			               .providerId(userInfo.getProviderId())
			               .build();
		
		User saved = userRepository.save(newUser);
		
		forceRoleLoad(saved);
		
		return saved;
	}
	
	/** this is helper method to overcome the lazy-loading for the Role property in User */
	private void forceRoleLoad(User user) {
		Hibernate.initialize(user.getRole());
	}
}
