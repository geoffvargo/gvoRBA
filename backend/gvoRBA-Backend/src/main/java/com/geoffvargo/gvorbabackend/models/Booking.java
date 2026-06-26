package com.geoffvargo.gvorbabackend.models;

import java.time.*;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "bookings")
public class Booking {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", referencedColumnName = "id", nullable = false)
	private Room roomId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
	private User userId;
	
	@Column(name = "starts_at", nullable = false)
	private LocalDateTime startsAt;
	
	@Column(name = "ends_at", nullable = false)
	private LocalDateTime endsAt;
	
	@Column(name = "cancelled_at", nullable = false)
	private LocalDateTime cancelledAt;
	
	@Column(name = "purpose")
	private String purpose;
	
	@Column(name = "status")
	private BookingStatus status;
}
