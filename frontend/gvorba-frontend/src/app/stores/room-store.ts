import { effect, inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { Room } from '../models/room.model';
import { Booking } from '../models/booking.model';
import { CreateRoomRequest } from '../models/create-room.request';
import { UpdateRoomRequest } from '../models/update-room.request';

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
	
	constructor() {
		effect(() => {
			const id = this._selectedRoom()?.id;
			const date = this._selectedDate();
			
			if (id != null) {
				this.loadRoomBookings(id, date);
			}
		});
	}
	
	setSelectedDate(date: Date) {
		this._selectedDate.set(date);
	}
	
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
	
	updateRoom(id: number, payload: UpdateRoomRequest) {
		this._isLoading.set(true);
		this.apiService.updateRoom(id, payload).subscribe({
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
	
	deactivateRoom(id: number) {
		this._isLoading.set(true);
		this.apiService.deactivateRoom(id).subscribe({
			next: room => {
				console.log(room);
				this._isLoading.set(false);
			},
			error: err => {
				console.error(err);
				this._isLoading.set(false);
			},
		});
	}
	
	reset() {
		this._rooms.set([]);
		this._selectedRoom.set(null);
		this._roomBookings.set([]);
		this._selectedDate.set(new Date());
		this._isLoading.set(false);
	}
}
