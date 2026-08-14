import { Component, inject } from '@angular/core';
import { Location } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { UserStore } from '../../stores/user-store';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatLabel } from '@angular/material/input';
import { MatOption } from '@angular/material/core';
import { MatSelect } from '@angular/material/select';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { UserCreationModel } from '../../models/user-creation.model';

@Component({
	selector: 'app-admin-add-user',
	imports: [
		ReactiveFormsModule,
		MatLabel,
		MatOption,
		MatSelect,
		MatSlideToggle,
	],
	templateUrl: './admin-add-user.component.html',
	styleUrl: './admin-add-user.component.css',
})
export class AdminAddUserComponent {
	private router = inject(Router);
	private route = inject(ActivatedRoute);
	private location = inject(Location);
	
	protected userStore = inject(UserStore);
	
	isLoading = this.userStore.isLoading;
	
	roles = ['ROLE_GUEST', 'ROLE_USER', 'ROLE_ADMIN'];
	
	userForm = new FormGroup({
		name: new FormControl(''),
		email: new FormControl(''),
		role: new FormControl(this.roles[0]),
		password: new FormControl(''),
		enabled: new FormControl(true),
	});
	
	async onSave() {
		const raw = this.userForm.getRawValue();
		const user: UserCreationModel = {
			name: raw.name!,
			email: raw.email!,
			password: raw.password!,
			role: this.userStore.roleChooser(raw.role ?? 'ROLE_GUEST'),
			enabled: raw.enabled!,
		};
		
		console.log('user: ', user);
		await this.userStore.createUser(user);
		
		this.location.back();
	}
	
	onCancel() {
		this.location.back();
	}
}
