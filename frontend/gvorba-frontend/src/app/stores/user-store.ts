import { inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { User } from '../models/user.model';

@Injectable({
	providedIn: 'root',
})
export class UserStore {
	private apiService = inject(ApiService);
	
	private _users = signal<User[]>([]);
	private _isLoading = signal<boolean>(false);
	
	readonly users = this._users.asReadonly();
	readonly isLoading = this._isLoading.asReadonly();
	
	loadUsers() {
		this._isLoading.set(false);
		// this.apiService.l
	}
}
