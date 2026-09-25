package com.geoffvargo.gvorbabackend.security.oauth2;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.services.*;

import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.user.*;
import org.springframework.stereotype.*;

import lombok.*;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
	private final OAuth2UserProvisioningService provisioningService;
	
	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);
		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		OAuth2UserInfo info = OAuth2UserInfoFactory.create(registrationId, oAuth2User.getAttributes());
		provisioningService.provision(AuthProvider.fromRegistrationId(registrationId), info);
		
		return oAuth2User;
	}
}
