import { Component, inject, OnInit, signal } from '@angular/core';
import { UserStore } from '../../stores/user-store';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { MatOption } from '@angular/material/core';
import { MatLabel, MatSelect } from '@angular/material/select';
import { UserUpdate } from '../../models/user-update.model';

@Component({
	selector: 'app-user-details',
	imports: [
		ReactiveFormsModule,
		MatSlideToggle,
		MatOption,
		MatSelect,
		MatLabel,
	],
	templateUrl: './user-admin-details.component.html',
	styleUrl: './user-admin-details.component.css',
})
export class UserAdminDetailsComponent implements OnInit {
	protected userStore = inject(UserStore);
	protected router = inject(Router);
	protected activatedRoute = inject(ActivatedRoute);
	
	readonly isLoading = this.userStore.isLoading;
	
	id = signal<number>(this.activatedRoute.snapshot.params['id']);
	isEditing = signal(false);
	updateReq = signal<UserUpdate>(new UserUpdate());
	
	readonly user = this.userStore.loadedUser;
	
	roles = ['ROLE_GUEST', 'ROLE_USER', 'ROLE_ADMIN'];
	
	editingForm = new FormGroup({
		name: new FormControl(''),
		email: new FormControl(''),
		role: new FormControl(this.roles[0]),
		enabled: new FormControl(false),
	});
	
	ngOnInit() {
		this.userStore.loadUser(this.id());
		console.log('user: ', this.user());
		this.populateForm();
	}
	
	onBack() {
		this.router.navigate(['..'], {
			relativeTo: this.activatedRoute,
		}).then();
	}
	
	toggleEdit() {
		this.populateForm();
		this.isEditing.update(state => !state);
	}
	
	onCancel() {
		console.log('CANCELED!');
		this.toggleEdit();
	}
	
	onSave() {
		this.updateReq.update(curr => ({
			// ...curr,
			email: this.editingForm.controls['email'].value ?? curr.email,
			name: this.editingForm.controls['name'].value ?? curr.name,
			role: this.userStore.roleChooser(this.editingForm.controls['role'].value ?? ''),
			enabled: this.editingForm.controls['enabled'].value ?? curr.enabled,
		}));
		
		console.log(this.updateReq());
		
		this.userStore.updateUser(this.id(), this.updateReq());
		this.toggleEdit();
	}
	
	onReset() {
		console.log('RESET!');
		this.populateForm();
	}
	
	populateForm() {
		const user = this.user();
		if (user == null || user.role === undefined) {
			return;
		}
		this.editingForm.patchValue({
			name: this.user()?.name?.toString(),
			email: this.user()?.email?.toString(),
			role: this.roles[user.role.id],
			enabled: this.user()?.enabled,
		});
		console.log(this.editingForm.get('role')!.value, typeof this.editingForm.get('role')!.value);
	}
}
