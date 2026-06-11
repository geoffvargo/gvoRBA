package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.models.*;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.*;

@RestController
@RequestMapping("/api")
public class MiscController {
	@GetMapping("/health")
	public ResponseEntity<?> getHealth() {
		return ResponseEntity.ok("I'M ALIVE!");
	}
	
	@GetMapping("/ping")
	public ResponseEntity<?> getPing() {
		String status = "ok";
		String timestamp = LocalDateTime.now().toString();
		PingResponse pingResponse = new PingResponse(status, timestamp);
		return ResponseEntity.ok(pingResponse);
	}
}
