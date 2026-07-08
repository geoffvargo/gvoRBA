import { inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { User } from '../models/user.model';
import { Role } from '../models/role.model';

@Injectable({
	providedIn: 'root',
})
export class AuthStore {
	private apiService = inject(ApiService);
	
	private _user = signal<User>(new User());
	private _role = signal<Role>(new Role());
	private _authToken = signal('');
	
	readonly user = this._user.asReadonly();
	readonly role = this._role.asReadonly();
	readonly authToken = this._authToken.asReadonly();
	
	constructor() {
		const token = sessionStorage.getItem('auth-token');
		
		if (token) {
			this.apiService.getCurrentUser().subscribe({
				next: (data: User) => {
					this._user.set(data);
					console.log(this._user());
					this._role.set(this._user().role);
					this._authToken.set(token);
				},
				error: err => {
					console.log(err);
					this.resetState();
				},
			});
		}
	}
	
	private resetState() {
		sessionStorage.removeItem('auth-token');
		this._user.set(new User());
		this._role.set(new Role());
		this._authToken.set('');
	}
}
