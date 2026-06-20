package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.repos.*;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "http://localhost:4200")
public class RoomController {
	public static final Logger LOGGER = LoggerFactory.getLogger(RoomController.class);
	
	@Autowired
	private RoomRepository roomRepository;
	
	@GetMapping()
	public ResponseEntity<?> getAllRooms(){
		return ResponseEntity.ok(roomRepository.findAll());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<?> getRoomById(@PathVariable Long id){
		Room room = roomRepository.findById(id).orElseThrow(
			() -> new UsernameNotFoundException("Room with id " + id + " not found")
		);
		
		return ResponseEntity.ok(room);
	}
}
