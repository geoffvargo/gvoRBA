package com.geoffvargo.gvorbabackend.services.impl;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
	
	@Mock
	private UserRepository userRepository;
	
	@InjectMocks
	private UserServiceImpl userServiceImpl;
	
	@BeforeEach
	void setUp() {
	}
	
	@AfterEach
	void tearDown() {
	}
	
	@Test
	void createUser() {
		new User();
		User user = User.builder()
			            .email("adf")
			            .password("adfadf")
			            .name("qerqer")
			            .role(new Role(AppRole.ROLE_USER))
			            .createdOn(new Date())
			            .build();
		User res = userRepository.save(user);
	}
	
	@Test
	void getUserById() {
	}
}
