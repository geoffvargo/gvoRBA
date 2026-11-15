package com.geoffvargo.gvorbabackend.security;

import com.geoffvargo.gvorbabackend.security.jwt.*;

import org.jspecify.annotations.*;
import org.springframework.security.authorization.*;
import org.springframework.security.core.*;
import org.springframework.stereotype.*;

import java.util.function.*;

import lombok.*;
import lombok.extern.slf4j.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TotpFactorAuthorizationManager implements AuthorizationManager<Object> {
	private final AuthorizationManager<Object> requireTotp = AllAuthoritiesAuthorizationManager.hasAllAuthorities(
		Factors.TOTP);
	
	@Override
	public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication,
	                                               Object context) {
		Authentication supplied = authentication.get();
		
		if (requiresTotp(supplied)) {
			return requireTotp.authorize(authentication, context);
		}
		
		return new AuthorizationDecision(true);
	}
	
	public static boolean requiresTotp(@Nullable Authentication authentication) {
		return authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl &&
		       ((UserDetailsImpl) authentication.getPrincipal()).isMultifactor();
	}
	
	public static boolean hasTotp(Authentication authentication) {
		return authentication.getAuthorities().contains(Factors.TOTP);
	}
}
