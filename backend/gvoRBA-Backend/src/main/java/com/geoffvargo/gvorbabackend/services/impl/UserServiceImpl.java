package com.geoffvargo.gvorbabackend.services.impl;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.services.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import lombok.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;
	
	@Override
	public User createUser(User user) {
		return userRepository.save(user);
	}
	
	@Override
	public User getUserById(Long id) {
		return userRepository.getUserById(id);
	}
}
