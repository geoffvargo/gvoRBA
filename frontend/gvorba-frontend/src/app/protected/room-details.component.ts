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
	
	constructor() {
		effect(() => {
			let data: Booking[] = this.roomBookings();
			console.log("roomBookings: {}", data);
		});
	}
	
	ngOnInit() {
		if (!this.roomId()) {
			return;
		}
		
		this.roomStore.loadRoom(this.roomId());
	}
	
	onBack() {
		void this.router.navigate(['..'],
			{ relativeTo: this.activatedRoute },
		);
	}
}
