import { effect, inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { Room } from '../models/room.model';
import { Booking } from '../models/booking.model';
import { CreateRoomRequest } from '../models/create-room.request';
import { UpdateRoomRequest } from '../models/update-room.request';
import { tap } from 'rxjs';
import { Amenities } from '../models/amenities.enum';
import { HotToastService } from '@ngxpert/hot-toast';

/**
 * App-wide store for rooms: the room list, the room being viewed, and that room's bookings.
 *
 * State lives in private writable signals. Components read the `readonly` versions and change
 * state only through the methods below. Every API call sets `isLoading` while it runs, and
 * failures show an error toast.
 */
@Injectable({
	providedIn: 'root',
})
export class RoomStore {
	private apiService = inject(ApiService);
	private toast = inject(HotToastService);

	private _rooms = signal<Room[]>([]);
	private _selectedRoom = signal<Room | null>(null);       // set by loadRoom(); drives the bookings effect
	private _roomBookings = signal<Booking[]>([]);           // bookings for _selectedRoom around _selectedDate
	private _selectedDate = signal<Date>(new Date());        // start of the bookings window; defaults to "now"
	private _isLoading = signal<boolean>(false);
	private _amenities = signal(Object.values(Amenities));   // every Amenities value, for amenity pickers

	// Read-only views for components.
	readonly rooms = this._rooms.asReadonly();
	readonly selectedRoom = this._selectedRoom.asReadonly();
	readonly roomBookings = this._roomBookings.asReadonly();
	readonly selectedDate = this._selectedDate.asReadonly();
	readonly isLoading = this._isLoading.asReadonly();
	readonly amenities = this._amenities.asReadonly();

	constructor() {
		// Load the room list once, when the store is first injected.
		// loadRooms() already sets _rooms in its tap(), so this next handler sets it a second time.
		this.loadRooms().subscribe({
			next: data => {
				this._rooms.set(data);
			},
			error: err => {
				console.error(err);
			},
		});

		// Reload the selected room's bookings whenever the selected room or the selected date
		// changes. Components never call loadRoomBookings() themselves. They change one of these
		// two signals, and this effect does the rest.
		effect(() => {
			const id = this._selectedRoom()?.id;
			const date = this._selectedDate();

			if (id != null) {
				this.loadRoomBookings(id, date);
			}
		});
	}

	/** Moves the bookings window. The effect in the constructor reloads roomBookings. */
	setSelectedDate(date: Date) {
		this._selectedDate.set(date);
	}

	/**
	 * Fetches all rooms and stores them in `rooms`. Returns the observable, so the caller must
	 * subscribe, or nothing is fetched.
	 *
	 * `name` and `minCapacity` are meant to filter the list, but they currently have no effect:
	 * `Array.filter()` returns a new array, and the results below are thrown away.
	 */
	loadRooms(name?: string, minCapacity?: number) {
		this._isLoading.set(true);
		return this.apiService.getRooms().pipe(
			tap({
				next: rooms => {
					const rmList = rooms;
					if (name) {
						rmList.filter((r) => r.name.includes(name));
					}

					if (minCapacity) {
						rmList.filter(r => r.capacity >= minCapacity);
					}

					this._rooms.set(rmList);
					this._isLoading.set(false);
				},
				error: err => {
					console.error(err);
					this.toast.error(err.message, {
						position: 'bottom-center',
						dismissible: true,
					});
					this._isLoading.set(false);
				},
			}),
		);
	}

	/**
	 * Fetches one room and makes it the selected room. Setting `_selectedRoom` triggers the
	 * constructor's effect, which then loads that room's bookings.
	 */
	loadRoom(id: number) {
		this._isLoading.set(true);
		this.apiService.getRoom(id).pipe(
			// delay(5000),
		).subscribe({
			next: room => {
				this._selectedRoom.set(room);
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
		});
	}

	/**
	 * Fetches a room's bookings into `roomBookings`. Called by the constructor's effect.
	 *
	 * The backend returns the room's non-cancelled bookings that overlap the 5 days starting at
	 * `date`. ApiService sends `date` via `toISOString()`, which converts it to UTC, so the window
	 * starts at the UTC time, not the local one.
	 */
	loadRoomBookings(id: number, date: Date) {
		// Debug: logs the calling line, to show what triggered this load.
		console.log(new Error().stack!.split('\n')[1].trim(), 'my message');
		this._isLoading.set(true);
		this.apiService.getRoomBookings(id.toString(), date).subscribe({
			next: bookings => {
				// console.log(new Error().stack!.split('\n')[1].trim(), 'my message');
				this._roomBookings.set(bookings);
				console.log('roomBookings: {}', this.roomBookings());
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
		});
	}

	/** Creates a room and appends the server's copy to `rooms`, without refetching the list. */
	createRoom(payload: CreateRoomRequest) {
		this._isLoading.set(true);
		this.apiService.createRoom(payload).subscribe({
			next: room => {
				this._rooms.set([...this._rooms(), room]);
				this.toast.success('Room created successfully.', {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
		});
	}

	/**
	 * Updates a room, then swaps the server's updated copy into `rooms` by id.
	 * `selectedRoom` isn't updated, so a details page showing this room keeps the old values.
	 */
	updateRoom(id: number, payload: UpdateRoomRequest) {
		this._isLoading.set(true);
		this.apiService.updateRoom(id, payload).subscribe({
			next: room => {
				this._rooms.set(this._rooms().map(r => r.id === room.id ? room : r));
				this.toast.success('Room updated successfully.', {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
		});

		// this.loadRooms();
	}

	/**
	 * Deactivates a room on the server. Local state isn't changed, so `rooms` still holds the
	 * room's old copy until the list is reloaded.
	 */
	deactivateRoom(id: number) {
		this._isLoading.set(true);
		this.apiService.deactivateRoom(id).subscribe({
			next: room => {
				console.log(room);
				this.toast.success('Succesfully deactivated', {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
		});
	}

	/** Clears all state back to its initial values. Nothing calls this yet. */
	reset() {
		this._rooms.set([]);
		this._selectedRoom.set(null);
		this._roomBookings.set([]);
		this._selectedDate.set(new Date());
		this._isLoading.set(false);
	}
}
