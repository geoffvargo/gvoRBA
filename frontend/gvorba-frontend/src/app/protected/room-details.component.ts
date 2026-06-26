import { Component, inject, OnInit, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { Room } from '../models/room.model';
import { ActivatedRoute } from '@angular/router';

@Component({
	selector: 'app-room-details',
	imports: [],
	templateUrl: './room-details.component.html',
	styleUrl: './room-details.component.css',
})
export class RoomDetailsComponent implements OnInit {
	private apiService = inject(ApiService);
	private activatedRoute = inject(ActivatedRoute);
	
	roomId = signal<number>(this.activatedRoute.snapshot.params['id']);
	
	room = signal<Room>(new Room());
	
	ngOnInit() {
		this.apiService.getRoom(this.roomId()).subscribe({
			next: (room: Room) => {
				this.room.set(room);
				console.log(room);
			},
		});
	}
}
