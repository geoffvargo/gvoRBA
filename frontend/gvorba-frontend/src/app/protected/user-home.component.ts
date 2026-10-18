import { Component, computed, inject, signal } from '@angular/core';
import { User } from '../models/user.model';
import { AvatarComponent } from 'ngx-avatars';
import { UserStore } from '../stores/user-store';
import { NonNullableFormBuilder, Validators } from '@angular/forms';

@Component({
	selector: 'app-user-home',
	imports: [
		AvatarComponent,
	],
	templateUrl: './user-home.component.html',
	styleUrl: './user-home.component.css',
})
export class UserHomeComponent {
	private userStore = inject(UserStore);
	
	isAdmin = this.userStore.isAdmin;
	
	isEditing = signal(false);
	
	// Falls back to an empty `User` while the store's user is still loading (or after logout), so the template never sees
	// `null`.
	currUser = computed<User>(() => this.userStore.user() ?? new User());
	
	private readonly fb = inject(NonNullableFormBuilder);
	
	userDetailsForm = this.fb.group({
		name: ['', Validators.required],
		email: ['', Validators.required],
	});
	
	userPreferencesForm = this.fb.group({
		emailNotification: [false, Validators.required],
		multifactor: [false, Validators.required],
		confirmBookings: [false, Validators.required],
		notifyOnAdminCancel: [false, Validators.required],
		favoriteRoons: [[], Validators.required],
	});
	
	onEdit() {
		console.log('user edited');
		this.isEditing.set(true);
	}
	
	onCancel() {
		this.isEditing.set(false);
	}
}
