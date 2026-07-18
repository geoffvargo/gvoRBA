import { Component, effect, inject, OnInit, signal, ViewEncapsulation } from '@angular/core';
import { ApiService } from '../services/api.service';
import { ActivatedRoute, Router } from '@angular/router';
import { RoomStore } from '../stores/room-store';
import { Booking } from '../models/booking.model';

@Component({
	selector: 'app-room-details',
	imports: [],
	templateUrl: './room-details.component.html',
	styleUrl: './room-details.component.css',
	encapsulation: ViewEncapsulation.None,
})
export class RoomDetailsComponent implements OnInit {
	private apiService = inject(ApiService);
	private roomStore = inject(RoomStore);
	private router = inject(Router);
	private activatedRoute = inject(ActivatedRoute);
	
	protected roomId = signal<number>(this.activatedRoute.snapshot.params['id']);
	
	protected room = this.roomStore.selectedRoom;
	protected isLoading = this.roomStore.isLoading;
	protected roomBookings = this.roomStore.roomBookings;
	protected selectedDate = this.roomStore.selectedDate;
	protected readonly Date = Date;
	
	constructor() {
		effect(() => {
			const data: Booking[] = this.roomBookings();
			console.log("roomBookings: {}", data);
		});
	}
	
	ngOnInit() {
		if (!this.roomId()) {
			return;
		}
		
		this.roomStore.loadRoom(this.roomId());
	}
	
	timeMapper(date: Date) {
		if (typeof date !== 'object') {
			date = new Date(date);
		}
		
		const time = date.toLocaleTimeString('en-GB');
		
		return this.rowGridMapper(time);
	}
	
	dayMapper(date: Date) {
		if (typeof date !== 'object') {
			date = new Date(date);
		}
		
		const day = date.getDay();
		
		return day;
	}
	
	onBack() {
		void this.router.navigate(['..'],
			{ relativeTo: this.activatedRoute },
		);
	}
	
	protected rowGridMapper(time: string) {
		const hours = Number(time.slice(0, 2));
		const minutes = Number(time.slice(3, 5));
		
		return (hours - 7) * 4 + Math.round(minutes / 15) - 1;
	}
}
