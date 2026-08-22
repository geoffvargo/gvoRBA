import { Component, computed, effect, inject, OnInit, signal, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RoomStore } from '../../stores/room-store';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Amenities } from '../../models/amenities.enum';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { UpdateRoomRequest, UpdateRoomRequestImpl } from '../../models/update-room.request';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
	selector: 'app-room-manage',
	imports: [
		ReactiveFormsModule,
		MatSlideToggle,
	],
	templateUrl: './room-manage.component.html',
	styleUrl: './room-manage.component.css',
	encapsulation: ViewEncapsulation.None,
})
export class RoomManageComponent implements OnInit {
	private router = inject(Router);
	private route = inject(ActivatedRoute);
	
	protected roomStore = inject(RoomStore);
	
	protected isEditing = signal(false);
	protected isLoading = this.roomStore.isLoading;
	protected roomId = signal<number>(this.route.snapshot.params['id']);
	protected room = this.roomStore.selectedRoom;
	protected updateReq = signal<UpdateRoomRequest>(new UpdateRoomRequestImpl());
	protected readonly amenityOptions = Object.values(Amenities);
	protected readonly roomPayload = computed(() => {
		const { amenities, ...rest } = this.formValue();
		return {
			...rest,
			amenities: Object.values(Amenities).filter(a => amenities?.[a]),
		} as UpdateRoomRequestImpl;
	});
	roomEditForm = new FormGroup({
			name: new FormControl(''),
			location: new FormControl(''),
			capacity: new FormControl(0),
			description: new FormControl(''),
			// ---- the nested checkbox group ----
			amenities: new FormGroup(
				Object.fromEntries(
					Object.values(Amenities).map(a => [
							a, new FormControl(false, { nonNullable: true }),
						],
					),
				) as Record<Amenities, FormControl<boolean>>,
			),
			isActive: new FormControl(true),
		},
	);
	protected readonly formValue = toSignal(
		this.roomEditForm.valueChanges, {
			initialValue: this.roomEditForm.value,
		},
	);
	
	constructor() {
		effect(() => this.populateRoom());
	}
	
	ngOnInit() {
		if (!this.roomId()) {
			return;
		}
		
		this.roomStore.loadRoom(this.roomId());
	}
	
	populateRoom() {
		const room = this.room();
		
		if (room == null) {
			return;
		}
		
		const amenities = Object.fromEntries(
			Object.values(Amenities).map(a => [a, room.amenities.includes(a)]),
		) as Record<Amenities, boolean>;
		
		this.roomEditForm.patchValue({
			name: room.name,
			location: room.location,
			capacity: room.capacity,
			amenities,
			isActive: room.isActive,
		});
	}
	
	onEdit() {
		this.isEditing.update(state => !state);
	}
	
	onSave() {
		this.updateReq.set({
			name: this.roomEditForm.controls.name.value,
			location: this.roomEditForm.controls.location.value,
			capacity: this.roomEditForm.controls.capacity.value,
			amenities: this.fromAmenityMap(this.roomEditForm.controls.amenities.getRawValue()),
			isActive: this.roomEditForm.controls.isActive.value,
		} as UpdateRoomRequest);
		
		console.log('updateReq: ', this.updateReq());
		
		this.roomStore.updateRoom(this.roomId(), this.updateReq());
		
		this.onBack();
	}
	
	onReset() {
		this.populateRoom();
	}
	
	onBack() {
		this.router.navigate(['..'], {
			relativeTo: this.route,
		}).then();
	}
	
	private fromAmenityMap(map: Record<Amenities, boolean>) {
		return (Object.entries(map) as [Amenities, boolean][])
			.filter(([, checked]) => checked)
			.map(([key]) => key);
	}
	
}
