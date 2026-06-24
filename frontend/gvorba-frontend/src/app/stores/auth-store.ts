import { inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { User } from '../models/user.model';
import { Role } from '../models/role.model';

@Injectable({
	providedIn: 'root',
})
export class AuthStore {
	private apiService = inject(ApiService);
	
	protected user = signal<User>(new User());
	protected role = signal<Role>(new Role());
	protected authToken = signal('');
	
	constructor() {
		const token = sessionStorage.getItem('auth-token');
		
		if (token) {
			this.apiService.getCurrentUser().subscribe({
				next: (data: User) => {
					this.user.set(data);
					console.log(this.user());
					this.role.set(this.user().role);
					this.authToken.set(token);
				},
			});
		}
	}
	
	getUser() {
		return this.user();
	}
	
	getRole() {
		return this.role();
	}
	
	
}
