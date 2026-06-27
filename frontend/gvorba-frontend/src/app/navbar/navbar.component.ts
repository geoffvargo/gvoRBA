import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { ApiService } from '../services/api.service';
import { JwtHelperService } from '@auth0/angular-jwt';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

@Component({
	selector: 'app-navbar',
	imports: [
		RouterLink,
	],
	templateUrl: './navbar.component.html',
	styleUrl: './navbar.component.css',
})
export class NavbarComponent implements OnInit, OnDestroy {
	private subscription!: Subscription;
	private apiService = inject(ApiService);
	private jwtHelper = inject(JwtHelperService);
	
	protected router = inject(Router);
	
	isAdmin = signal(false);
	isLoggedIn = signal(false);
	
	authToken = signal(sessionStorage.getItem('auth-token'));
	menuVisible = signal(false);
	role = signal('');
	
	ngOnInit() {
		this.subscription = this.apiService.isloggedIn$.subscribe(() => {
			this.refreshAuthState();
		});
		
		this.refreshAuthState();
	}
	
	ngOnDestroy() {
		this.subscription?.unsubscribe();
	}
	
	onLogout() {
		this.apiService.logout();
		this.router.navigateByUrl('').then();
	}
	
	private refreshAuthState() {
		const token = sessionStorage.getItem('auth-token');
		this.authToken.set(token);
		
		if (!token || this.jwtHelper.isTokenExpired(token)) {
			this.isLoggedIn.set(false);
			this.isAdmin.set(false);
			this.role.set('');
			
			return;
		}
		
		const payload = this.decodeJwt(token);
		this.role.set(payload.roles);
		
		this.isLoggedIn.set(true);
		this.isAdmin.set(payload.roles === 'ROLE_ADMIN');
	}
	
	private decodeJwt(token: string): { roles: string } {
		const payload = token.split('.')[1];
		const decodedPayload = atob(payload);
		return JSON.parse(decodedPayload);
	}
}
