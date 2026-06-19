package com.geoffvargo.gvorbabackend.security.jwt;

import com.geoffvargo.gvorbabackend.models.*;

import org.junit.jupiter.api.*;
import org.slf4j.*;

import static org.junit.jupiter.api.Assertions.*;

class UserDetailsImplTest {
	public static final Logger LOGGER = LoggerFactory.getLogger(UserDetailsImplTest.class);
	
	@Test
	void build() {
		User user = User.builder()
			            .id(1L)
			            .name("Jane Doe")
			            .email("jane@example.com")
			            .password("hashedPassword")
			            .role(new Role(AppRole.ROLE_USER))
			            .build();
		
		UserDetailsImpl details = UserDetailsImpl.build(user);
		
		LOGGER.info(details.toString());
		
		assertEquals(user.getId(), details.getId());
		assertEquals(user.getName(), details.getUsername());
		assertEquals(user.getEmail(), details.getEmail());
		assertEquals(user.getPassword(), details.getPassword());
	}
}
