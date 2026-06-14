package com.geoffvargo.gvorbabackend.models;

import java.time.*;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "bookings")
public class Booking {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(nullable = false)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", referencedColumnName = "id", nullable = false)
	private Room roomId;
	
	private LocalDateTime startsAt;
	
	private LocalDateTime endsAt;
	
	private String purpose;
	
	private BookingStatus status;
}
