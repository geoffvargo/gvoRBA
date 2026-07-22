package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.models.DTOs.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.models.User;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.security.jwt.*;

import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.logging.*;

import lombok.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@CrossOrigin("http://localhost:4200")
public class AdminController {
	public static final Logger LOGGER = Logger.getLogger(AdminController.class.getName());
	
	private final UserRepository userRepository;
	
	private final RoleRepository roleRepository;
	
	@GetMapping("/")
	public ResponseEntity<List<User>> loadAllUsers() {
		return ResponseEntity.ok(userRepository.findAll());
	}
	
	@PatchMapping("/{id}/role")
	public ResponseEntity<Role> updateRole(@AuthenticationPrincipal UserDetails userDetails,
	                                       @PathVariable Long id,
	                                       @RequestBody RoleDto roleInput) {
		
		if (Objects.equals(((UserDetailsImpl) userDetails).getId(), id)) {
			throw new DataIntegrityViolationException("Cannot self-promote/demote!");
		}
		
		User user = userRepository.findById(id).orElseThrow(
			() -> new UsernameNotFoundException("User not found!")
		);
		
		Role role = roleRepository.findByRoleName(roleInput.roleName()).orElseThrow(
			() -> new RuntimeException("Role not found!")
		);
		
		user.setRole(role);
		userRepository.save(user);
		
		return ResponseEntity.ok(role);
	}
	
	@PatchMapping("/{id}/toggle-active")
	public ResponseEntity<User> toggleActive(@AuthenticationPrincipal UserDetails userDetails,
	                                         @PathVariable Long id) {
		if (Objects.equals(((UserDetailsImpl) userDetails).getId(), id)) {
			throw new DataIntegrityViolationException("Cannot self-activate/deactivate!");
		}
		
		User user = userRepository.findById(id).orElseThrow(
			() -> new UsernameNotFoundException("User not found!")
		);

//		if (user.) {}
		return null;
	}
}
