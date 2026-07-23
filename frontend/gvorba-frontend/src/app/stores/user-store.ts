import { inject, Injectable, OnInit, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { User } from '../models/user.model';
import { Role } from '../models/role.model';

@Injectable({
	providedIn: 'root',
})
export class UserStore implements OnInit {
	private apiService = inject(ApiService);
	
	private _users = signal<User[]>([]);
	private _isLoading = signal<boolean>(false);
	
	readonly users = this._users.asReadonly();
	readonly isLoading = this._isLoading.asReadonly();
	
	ngOnInit() {
		this.loadUsers();
	}
	
	loadUsers() {
		this._isLoading.set(false);
		this.apiService.loadUsers().subscribe({
			next: data => {
				this._users.set(data);
				console.log(data);
				this._isLoading.set(false);
			},
			error: err => {
				console.log(err);
				this._isLoading.set(false);
			},
		});
	}
	
	updateRole(id: number, role: Role) {
		this._isLoading.set(false);
		this.apiService.updateRole(id, role).subscribe({
			next: data => {
				console.log(data);
				this._isLoading.set(false);
			},
			error: err => {
				console.log(err);
				this._isLoading.set(false);
			},
		});
	}
	
	toggleActive(id: number) {
		this._isLoading.set(false);
		this.apiService.toggleActive(id).subscribe({
			next: data => {
				console.log(data);
				this._isLoading.set(false);
			},
			error: err => {
				console.log(err);
				this._isLoading.set(false);
			},
		});
	}
}
