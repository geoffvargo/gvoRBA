package com.geoffvargo.gvorbabackend.models;

import com.fasterxml.jackson.annotation.*;

import java.util.*;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_prefs")
public class UserPrefs {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;

	@Builder.Default
	@Column(name = "email_notification", nullable = false)
	private Boolean emailNotification = false;

	@Builder.Default
	@Column(name = "multifactor", nullable = false)
	private Boolean multifactor = false;

	@Builder.Default
	@Column(name = "confirm_bookings", nullable = false)
	private Boolean confirmBookings = false;

	@Builder.Default
	@Column(name = "notify_on_admin_cancel", nullable = false)
	private Boolean notifyOnAdminCancel = false;

	@OneToOne(optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	@JsonIgnore
	@ToString.Exclude
	private User user;

	@Builder.Default
	@ElementCollection
	@Column(name = "room_id")
	@CollectionTable(name = "user_prefs_room_ids", joinColumns = @JoinColumn(name = "owner_id"))
	private Set<Long> favoriteRooms = new LinkedHashSet<>();
}
