import { Component, inject, ViewEncapsulation } from '@angular/core';
import { RoomStore } from '../../stores/room-store';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

@Component({
	selector: 'app-rooms-create',
	imports: [
		ReactiveFormsModule,
	],
	templateUrl: './rooms-create.component.html',
	styleUrl: './rooms-create.component.css',
	encapsulation: ViewEncapsulation.None,
})
export class RoomsCreateComponent {
	private router = inject(Router);
	private route = inject(ActivatedRoute);
	
	protected roomStore = inject(RoomStore);
	
	roomCreateForm = new FormGroup({
		name: new FormControl(''),
		location: new FormControl(''),
		capacity: new FormControl(0),
		amenities: new FormControl([]),
		isActive: new FormControl(false),
	});
}
