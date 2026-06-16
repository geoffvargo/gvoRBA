package com.geoffvargo.gvorbabackend.repos;

import com.geoffvargo.gvorbabackend.models.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;

import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	Boolean existsByName(String name);
	
	Boolean existsByEmail(String email);
	
	Optional<User> findByName(String name);
	
	User findByEmail(String email);
	
	User getUserById(Long id);
}
