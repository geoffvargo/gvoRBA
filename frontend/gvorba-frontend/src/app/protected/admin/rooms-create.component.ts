import { Component, computed, inject, signal, ViewEncapsulation } from '@angular/core';
import { RoomStore } from '../../stores/room-store';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Amenities } from '../../models/amenities.enum';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { CreateRoomRequest } from '../../models/create-room.request';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
	selector: 'app-rooms-create',
	imports: [
		ReactiveFormsModule,
		MatSlideToggle,
	],
	templateUrl: './rooms-create.component.html',
	styleUrl: './rooms-create.component.css',
	encapsulation: ViewEncapsulation.None,
})
export class RoomsCreateComponent {
	private router = inject(Router);
	private route = inject(ActivatedRoute);
	private readonly fb = inject(NonNullableFormBuilder);
	private readonly roomPayload = computed(() => {
		const { amenities, ...rest } = this.formValue();
		
		return {
			...rest,
			amenities: Object.values(Amenities).filter(a => amenities?.[a]),
		} as CreateRoomRequest;
	});
	protected roomStore = inject(RoomStore);
	readonly amenityOptions = Object.values(Amenities);
	readonly createReq = signal<CreateRoomRequest>(new CreateRoomRequest());
	roomCreateForm = new FormGroup({
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
	private readonly formValue = toSignal(
		this.roomCreateForm.valueChanges, {
			initialValue: this.roomCreateForm.value,
		},
	);
	
	onSave() {
		this.createReq.set(this.roomPayload());
		console.log(this.createReq());
		
		this.roomStore.createRoom(this.createReq());
		
		this.router.navigate(['..'], {
			relativeTo: this.route,
			replaceUrl: true,
		}).then();
	}
	
	onReset() {}
	
	onCancel() {
		this.router.navigate(['..'], {
			relativeTo: this.route,
			replaceUrl: true,
		}).then();
	}
}
