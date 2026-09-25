package com.geoffvargo.gvorbabackend.services;

import com.geoffvargo.gvorbabackend.exceptions.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.security.oauth2.*;

import org.hibernate.*;
import org.slf4j.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.stereotype.*;

import java.util.*;

import jakarta.transaction.*;
import lombok.*;

@Service
@RequiredArgsConstructor
public class OAuth2UserProvisioningService {
	public static final Logger LOGGER = LoggerFactory.getLogger(OAuth2UserProvisioningService.class);
	
	private final UserRepository userRepository;
	
	private final RoleRepository roleRepository;
	
	@Transactional
	public User provision(AuthProvider provider, OAuth2UserInfo userInfo) {
		Optional<User> existing = userRepository.findByAuthProviderAndProviderId(provider, userInfo.getProviderId());
		if (existing.isPresent()) {
			User user = existing.get();
			forceRoleLoad(user);
			return user;
		}

		String email = Optional.ofNullable(userInfo.getEmail()).orElse("").toLowerCase();

		if (email.isBlank()) {
			throw new OAuth2AuthException("Blank Email.");
		} else if (!userInfo.isEmailVerified()) {
			throw new OAuth2AuthenticationException("Email not verified.");
		}

		Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
		if (userOpt.isPresent()) {
			/// Link the existing account to this provider so the success handler's
			/// (provider, providerId) lookup finds it. Dirty-checked and flushed by @Transactional.
			User user = userOpt.get();
			user.setAuthProvider(provider);
			user.setProviderId(userInfo.getProviderId());
			forceRoleLoad(user);
			return user;
		}

		Role role;
		Optional<Role> roleOpt = roleRepository.findByRoleName(AppRole.ROLE_USER);
		if (roleOpt.isEmpty()) {
			throw new OAuth2AuthException("Role is empty!");
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
