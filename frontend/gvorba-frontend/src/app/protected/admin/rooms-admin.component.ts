import { Component, inject } from '@angular/core';
import { RoomStore } from '../../stores/room-store';

@Component({
  selector: 'app-rooms-admin',
  imports: [],
  templateUrl: './rooms-admin.component.html',
  styleUrl: './rooms-admin.component.css',
})
export class RoomsAdminComponent {
	protected roomStore = inject(RoomStore);
}
