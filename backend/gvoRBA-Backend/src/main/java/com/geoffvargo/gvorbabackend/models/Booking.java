package com.geoffvargo.gvorbabackend.models;

import java.time.*;
import java.util.*;

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
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Room room;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User userId;
	
	@Column(name = "starts_at", nullable = false)
	private LocalDateTime startsAt;
	
	@Column(name = "ends_at", nullable = false)
	private LocalDateTime endsAt;
	
	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;
	
	@Column(name = "purpose")
	private String purpose;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private BookingStatus status;
	
	@ManyToMany
	@JoinTable(name = "booking_attendee",
	           joinColumns = @JoinColumn(name = "booking_id"),
	           inverseJoinColumns = @JoinColumn(name = "user_id"))
	private Set<User> attendees = new HashSet<>();
}
