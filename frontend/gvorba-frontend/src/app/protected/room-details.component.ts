import { Component, effect, inject, OnInit, signal, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RoomStore } from '../stores/room-store';
import { Booking } from '../models/booking.model';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

/**
 * Shows one room's details and a Monday-Friday calendar of its bookings.
 *
 * Bookings are drawn as buttons inside the template's `#schedule-grid`, a CSS grid with
 * one column per weekday and 40 quarter-hour rows. The mapper methods below turn a
 * booking's start/end times into grid lines.
 */
@Component({
	selector: 'app-room-details',
	imports: [
		MatProgressSpinner,
	],
	templateUrl: './room-details.component.html',
	styleUrl: './room-details.component.css',
	encapsulation: ViewEncapsulation.None,
})
export class RoomDetailsComponent implements OnInit {
	private roomStore = inject(RoomStore);
	private router = inject(Router);
	private activatedRoute = inject(ActivatedRoute);
	
	// Room id from the `:id` route param. Read once from the snapshot, so it won't update
	// if the router reuses this component for a different room.
	protected roomId = signal<number>(this.activatedRoute.snapshot.params['id']);
	
	// All of these come from RoomStore. Loading the room sets selectedRoom, and the store's own
	// effect reloads roomBookings whenever the selected room or selectedDate changes.
	protected room = this.roomStore.selectedRoom;
	protected isLoading = this.roomStore.isLoading;
	protected roomBookings = this.roomStore.roomBookings;
	protected selectedDate = this.roomStore.selectedDate;
	
	// Exposes the global Date constructor to the template. The template doesn't use it at the moment.
	protected readonly Date = Date;
	
	constructor() {
		effect(() => {
			const data: Booking[] = this.roomBookings();
			console.log("roomBookings: {}", data);
		});
	}
	
	/** Loads the room from the route's id. Its bookings follow through RoomStore's effect. */
	ngOnInit() {
		if (!this.roomId()) {
			return;
		}
		this.roomStore.loadRoom(this.roomId());
	}
	
	/**
	 * Returns the `#schedule-grid` row line for a booking's start or end time.
	 * Used for both `grid-row-start` and `grid-row-end`, so a booking spans one row per 15 minutes.
	 */
	timeMapper(date: Date) {
		// Bookings come from JSON, so the "Date" is really an ISO string at runtime.
		if (typeof date !== 'object') {
			date = new Date(date);
		}
		
		// en-GB gives a 24-hour "HH:mm:ss" string in local time, which rowGridMapper slices up.
		const time = date.toLocaleTimeString('en-GB');
		
		return this.rowGridMapper(time);
	}
	
	/**
	 * Returns the `#schedule-grid` column for a booking's day. getDay() is 0 for Sunday,
	 * so Monday-Friday are 1-5, which line up with the grid's five weekday columns.
	 */
	dayMapper(date: Date) {
		// Same string-to-Date conversion as timeMapper.
		if (typeof date !== 'object') {
			date = new Date(date);
		}

		return date.getDay();
	}
	
	/** Goes up one route level, back to the rooms list. */
	onBack() {
		void this.router.navigate(['..'],
			{ relativeTo: this.activatedRoute },
		);
	}
	
	/** Opens the clicked booking's details page. */
	onBookingClick(id: number) {
		this.router.navigate(['bookings/', id]).then();
	}
	
	/**
	 * Converts an "HH:mm..." time string to a grid row line, with one row per 15 minutes.
	 *
	 * `(hours - 7) * 4` counts quarter-hours from 07:00, then `- 1` shifts it, so 08:00 maps to
	 * line 3 and each hour after that adds 4. Minutes are rounded to the nearest quarter-hour.
	 */
	protected rowGridMapper(time: string) {
		const hours = Number(time.slice(0, 2));
		const minutes = Number(time.slice(3, 5));
		
		return (hours - 7) * 4 + Math.round(minutes / 15) - 1;
	}
	
	/** Booking length in minutes, shown on each booking's button. */
	protected duration(booking: Booking) {
		// Wrapped in new Date() because startsAt and endsAt are strings at runtime (see timeMapper).
		const time1: number = new Date(booking.endsAt).getTime();
		const time2: number = new Date(booking.startsAt).getTime();
		return (time1 - time2) / 60_000;
	}
	
	onGoBackOneWeek() {
		const date = new Date(this.selectedDate());
		date.setDate(date.getDate() - 7);
		this.roomStore.setSelectedDate(date);
	}
	
	onGoForwardOneWeek() {
		const date = new Date(this.selectedDate());
		date.setDate(date.getDate() + 7);
		this.roomStore.setSelectedDate(date);
	}
	
	onResetCalendar() {
		this.roomStore.setSelectedDate(new Date());
	}
}
