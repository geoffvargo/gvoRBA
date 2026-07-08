import { inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { Room } from '../models/room.model';
import { Booking } from '../models/booking.model';
import { CreateRoomRequest } from '../models/create-room.request';

@Injectable({
	providedIn: 'root',
})
export class RoomStore {
	private apiService = inject(ApiService);
	
	private _rooms = signal<Room[]>([]);
	private _selectedRoom = signal<Room | null>(null);
	private _roomBookings = signal<Booking[]>([]);
	private _selectedDate = signal<Date>(new Date());
	private _isLoading = signal<boolean>(false);
	
	readonly rooms = this._rooms.asReadonly();
	readonly selectedRoom = this._selectedRoom.asReadonly();
	readonly roomBookings = this._roomBookings.asReadonly();
	readonly selectedDate = this._selectedDate.asReadonly();
	readonly isLoading = this._isLoading.asReadonly();
	
	loadRooms(name?: string, minCapacity?: number) {
		this._isLoading.set(true);
		this.apiService.getRooms().subscribe({
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
				this._isLoading.set(false);
			},
		});
	}
	
	loadRoom(id: number) {
		this._isLoading.set(true);
		this.apiService.getRoom(id).subscribe({
			next: room => {
				this._selectedRoom.set(room);
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this._isLoading.set(false);
			},
		});
	}
	
	loadRoomBookings(id: number, date: Date) {
		this._isLoading.set(true);
		this.apiService.getRoomBookings(id.toString(), date.toString()).subscribe({
			next: bookings => {
				this._roomBookings.set(bookings);
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this._isLoading.set(false);
			},
		});
	}
	
	createRoom(payload: CreateRoomRequest) {
		this._isLoading.set(true);
		this.apiService.createRoom(payload).subscribe({
			next: room => {
				this._rooms.set([...this._rooms(), room]);
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this._isLoading.set(false);
			},
		});
	}
}
