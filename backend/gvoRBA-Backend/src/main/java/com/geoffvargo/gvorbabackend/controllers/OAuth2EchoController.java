package com.geoffvargo.gvorbabackend.controllers;

import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dev")
@Profile("dev")
public class OAuth2EchoController {
	@GetMapping("/oauth2-echo")
	public Map<String, String> echo(@RequestParam Map<String, String> params) {
		return params;
	}
}
