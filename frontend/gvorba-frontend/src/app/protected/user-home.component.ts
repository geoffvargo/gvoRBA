import { Component, inject, OnInit, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { TokenStorageService } from '../services/token-storage-service';
import { JwtHelperService } from '@auth0/angular-jwt';
import { User } from '../models/user.model';

@Component({
	selector: 'app-user-home',
	imports: [],
	templateUrl: './user-home.component.html',
	styleUrl: './user-home.component.css',
})
export class UserHomeComponent implements OnInit {
	private apiService = inject(ApiService);
	private tokenService = inject(TokenStorageService);
	private jwtHelper = inject(JwtHelperService);
	
	token = signal(this.tokenService.getToken() ?? '');
	currUser = signal<User>(new User);
	
	decodeJwt(token: string): unknown {
		const payload = token.split('.')[1];
		const decodedPayload = atob(payload);
		console.log('[TARGET] decodedPayload: ' + decodedPayload);
		return JSON.parse(decodedPayload);
	}
	
	ngOnInit() {
		if (this.token() !== null) {
			this.apiService.getUser().subscribe({
				next: async (user: User) => {
					this.currUser.set(user);
					console.log('user: ', user);
				},
			});
		}
	}
}
