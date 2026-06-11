package com.geoffvargo.gvorbabackend.controllers;

import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MiscController {
	@GetMapping("/health")
	public ResponseEntity<?> getHealth() {
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
