package com.geoffvargo.gvorbabackend.services;

import com.geoffvargo.gvorbabackend.models.*;

public interface UserService {
	User createUser(User user);
	
	User getUserById(Long id);
}
