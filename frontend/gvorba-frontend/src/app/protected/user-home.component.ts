import { Component, computed, effect, inject, signal } from '@angular/core';
import { User } from '../models/user.model';
import { AvatarComponent } from 'ngx-avatars';
import { UserStore } from '../stores/user-store';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { Room } from '../models/room.model';
import { UserPrefs } from '../models/user.prefs';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { RoomStore } from '../stores/room-store';

@Component({
	selector: 'app-user-home',
	imports: [
		AvatarComponent,
		MatProgressSpinner,
		ReactiveFormsModule,
		MatSlideToggle,
	],
	templateUrl: './user-home.component.html',
	styleUrl: './user-home.component.css',
})
export class UserHomeComponent {
	private userStore = inject(UserStore);
	private roomStore = inject(RoomStore);
	
	isAdmin = this.userStore.isAdmin;
	isEditing = signal(false);
	isLoading = this.userStore.isLoading;
	currUser = computed<User>(() => this.userStore.user() ?? new User());
	userPrefs = this.userStore.userPrefs;
	rooms = this.roomStore.rooms;

	// Backend sends favorite rooms as IDs; resolve them to names for display.
	favoriteRoomNames = computed(() => {
		const favIds = new Set<unknown>(this.userPrefs().favoriteRooms);
		return this.rooms()
			.filter(room => favIds.has(room.id))
			.map(room => room.name)
			.join(', ');
	});

	private readonly fb = inject(NonNullableFormBuilder);
	
	constructor() {
		effect(() => {
			this.populateForms();
		});
	}
	
	userDetailsForm = this.fb.group({
		name: ['', Validators.required],
		email: ['', Validators.required],
	});
	
	protected readonly userDetailsValue = toSignal(
		this.userDetailsForm.valueChanges, {
			initialValue: this.userDetailsForm.value,
		});
	
	// private readonly userDetails = computed(() => {
	// 	const { name, email } = this.userDetailsForm.value;
	// });
	
	userPrefsForm = this.fb.group({
		emailNotification: [false, Validators.required],
		multifactor: [false, Validators.required],
		confirmBookings: [false, Validators.required],
		notifyOnAdminCancel: [false, Validators.required],
		notifyOnBookingChange: [false, Validators.required],
		notifyOnRoomDeactivated: [false, Validators.required],
		favoriteRooms: this.fb.control<Room[]>([], Validators.required),
	});
	
	protected readonly userPrefsFormValue = toSignal(
		this.userPrefsForm.valueChanges, {
			initialValue: this.userPrefsForm.value,
		},
	);
	
	private readonly userPrefsPayload = computed(() => {
		const {
			emailNotification, multifactor, confirmBookings, notifyOnAdminCancel, favoriteRooms, notifyOnBookingChange, notifyOnRoomDeactivated
		} = this.userPrefsFormValue();
		
		if (
			emailNotification == null ||
			multifactor == null ||
			confirmBookings == null ||
			notifyOnAdminCancel == null ||
			favoriteRooms == null ||
			notifyOnBookingChange == null ||
			notifyOnRoomDeactivated == null
		) {
			return null;
		}
		
		return {
			emailNotification,
			multifactor,
			confirmBookings,
			notifyOnAdminCancel,
			notifyOnBookingChange,
			notifyOnRoomDeactivated,
			favoriteRooms,
		} as UserPrefs;
	});
	
	populateForms() {
		this.userDetailsForm.patchValue({
			name: this.currUser().name,
			email: this.currUser().email,
		});
		
		const prefs = this.userPrefs();
		this.userPrefsForm.patchValue({
			emailNotification: prefs.emailNotification,
			multifactor: prefs.multifactor,
			confirmBookings: prefs.confirmBookings,
			notifyOnAdminCancel: prefs.notifyOnAdminCancel,
			notifyOnBookingChange : prefs.notifyOnBookingChange,
			notifyOnRoomDeactivated : prefs.notifyOnRoomDeactivated,
			favoriteRooms: prefs.favoriteRooms,
		});
	}
	
	onEdit() {
		console.log('user edited');
		this.isEditing.set(true);
	}
	
	onSave() {
		console.log('onSave {}', this.userPrefsPayload());
		const payload: null | UserPrefs = this.userPrefsPayload();
		
		if (!payload) {
			return;
		}
		
		this.userStore.setPrefs(this.currUser().id, payload);
		this.isEditing.set(false);
	}
	
	onReset() {
		console.log('onReset');
	}
	
	onCancel() {
		this.isEditing.set(false);
	}
}
