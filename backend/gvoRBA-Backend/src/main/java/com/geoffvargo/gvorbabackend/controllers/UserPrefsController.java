package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.models.DTOs.*;
import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;
import com.geoffvargo.gvorbabackend.security.jwt.*;

import org.springframework.http.*;
import org.springframework.security.core.annotation.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import lombok.*;
import lombok.extern.slf4j.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/userprefs")
@CrossOrigin(origins = "http://localhost:4200")
public class UserPrefsController {
	private final UserPrefsRepository prefsRepository;
	
	@GetMapping("/{id}/get-prefs")
	public ResponseEntity<?> getPrefs(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl userDetails) {
		UserPrefs prefs = prefsRepository.findByUserId(id);
		
		if (prefs == null) {
			return ResponseEntity.notFound()
				       .build();
		}
		
		if (!Objects.equals(userDetails.getId(), id) &&
		    !userDetails.getAuthorities().toArray()[0].toString().matches("ROLE_ADMIN")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				       .contentType(MediaType.TEXT_PLAIN)
				       .body("Insufficient privileges.");
		}
		
		return ResponseEntity.ok(prefs);
	}
	
	@PostMapping("/{id}/set-prefs")
	public ResponseEntity<?> setPrefs(@PathVariable Long id, @RequestBody UserPrefsDto prefsDTO,
	                                  @AuthenticationPrincipal UserDetailsImpl userDetails) {
		UserPrefs prefs = prefsRepository.findByUserId(id);
		
		if (prefs == null) {
			return ResponseEntity.notFound()
				       .build();
		}
		
		if (!Objects.equals(userDetails.getId(), id) &&
		    !userDetails.getAuthorities().toArray()[0].toString().matches("ROLE_ADMIN")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				       .contentType(MediaType.TEXT_PLAIN)
				       .body("Insufficient privileges.");
		}
		
		prefs.setConfirmBookings(prefsDTO.confirmBookings());
		prefs.setEmailNotification(prefsDTO.emailNotification());
		prefs.setFavoriteRooms(prefsDTO.favoriteRooms());
		prefs.setNotifyOnAdminCancel(prefsDTO.notifyOnAdminCancel());
		prefs.setMultifactor(prefsDTO.multifactor());
		
		prefsRepository.save(prefs);
		
		log.info(prefs.toString());
		
		return ResponseEntity.ok(prefs);
	}
}
