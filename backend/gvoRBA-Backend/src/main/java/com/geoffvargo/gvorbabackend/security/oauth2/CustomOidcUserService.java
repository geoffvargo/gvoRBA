package com.geoffvargo.gvorbabackend.security.oauth2;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.services.*;

import org.springframework.security.oauth2.client.oidc.userinfo.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.user.*;
import org.springframework.security.oauth2.core.user.*;
import org.springframework.stereotype.*;

import lombok.*;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {
	private final OAuth2UserProvisioningService provisioningService;
	
	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		OidcUser oidcUser = super.loadUser(userRequest);
		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		OAuth2UserInfo info = OAuth2UserInfoFactory.create(registrationId, oidcUser.getAttributes());
		provisioningService.provision(AuthProvider.fromRegistrationId(registrationId), info);

		return oidcUser;
	}
}
