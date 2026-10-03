import { inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { User } from '../models/user.model';
import { Role } from '../models/role.model';
import { UserUpdate } from '../models/user-update.model';
import { UserCreationModel } from '../models/user-creation.model';
import { firstValueFrom } from 'rxjs';
import { HotToastService } from '@ngxpert/hot-toast';

@Injectable({
	providedIn: 'root',
})
export class UserStore {
	private apiService = inject(ApiService);
	private toast = inject(HotToastService);
	
	private _users = signal<User[]>([]);
	private _isLoading = signal<boolean>(false);
	private _loadedUser = signal<User | null>(null);
	
	readonly users = this._users.asReadonly();
	readonly isLoading = this._isLoading.asReadonly();
	readonly loadedUser = this._loadedUser.asReadonly();
	
	constructor() {
		this.loadUsers();
	}
	
	loadUsers() {
		this._isLoading.set(true);
		this.apiService.loadUsers().subscribe({
			next: data => {
				this._users.set(data);
				console.log(data);
				this._isLoading.set(false);
			},
			error: err => {
				console.log(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
		});
	}
	
	loadUser(id: number) {
		this._isLoading.set(true);
		this.apiService.loadUser(id).subscribe({
			next: data => {
				this._loadedUser.set(data);
				console.log(this.loadedUser());
				this._isLoading.set(false);
			},
			error: err => {
				console.log(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
		});
	}
	
	updateRole(id: number, role: Role) {
		this._isLoading.set(true);
		this.apiService.updateRole(id, role).subscribe({
			next: data => {
				console.log(data);
				this.toast.success('User Role updated successfully.', {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
			error: err => {
				console.log(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
		});
	}
	
	toggleActive(id: number) {
		this._isLoading.set(true);
		this.apiService.toggleActive(id).subscribe({
			next: data => {
				console.log(data);
				this._isLoading.set(false);
			},
			error: err => {
				console.log(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
		});
	}
	
	updateUser(id: number, user: UserUpdate) {
		this._isLoading.set(true);
		this.apiService.updateUser(id, user).subscribe({
			next: data => {
				console.log(data);
				this.loadUser(id);
				this.toast.success('User updated successfully.', {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
			error: err => {
				console.log(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
		});
	}
	
	roleChooser(roleName: string) {
		switch (roleName) {
			case 'ROLE_GUEST':
				return { id: 0, roleName: 'ROLE_GUEST' };
			case 'ROLE_USER':
				return { id: 1, roleName: 'ROLE_USER' };
			case 'ROLE_ADMIN':
				return { id: 2, roleName: 'ROLE_ADMIN' };
			default:
				throw Error('not a valid role');
		}
	}
	
	async createUser(user: UserCreationModel): Promise<User | null> {
		this._isLoading.set(true);
		try {
			const created = await firstValueFrom(this.apiService.createUser(user)); // your existing Observable call
			this._users.update(list => [...list, created]);                  // your backing signal
			this.toast.success('User created successfully.', {
				position: 'bottom-center',
				dismissible: true,
			});
			return created;
		} catch (err) {
			console.log(err);
			this.toast.error(err instanceof Error ? err.message : 'User create failed', {
				position: 'bottom-center',
				dismissible: true,
			});
			return null;
		} finally {
			this._isLoading.set(false);
		}
	}
}
