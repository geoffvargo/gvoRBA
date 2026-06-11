package com.geoffvargo.gvorbabackend.controllers;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MiscController {
	@GetMapping("/health")
	public ResponseEntity<?> getHealth() {
		return ResponseEntity.ok("I'M ALIVE!");
	}
}
