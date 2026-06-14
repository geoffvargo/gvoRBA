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
	@Column(name = "id", nullable = false)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", referencedColumnName = "id", nullable = false)
	private Room roomId;
	
	@Column(name = "starts_at")
	private LocalDateTime startsAt;
	
	@Column(name = "ends_at")
	private LocalDateTime endsAt;
	
	@Column(name = "purpose")
	private String purpose;
	
	@Column(name = "status")
	private BookingStatus status;
}
