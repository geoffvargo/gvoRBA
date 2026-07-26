import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthStore } from '../stores/auth-store';

@Component({
	selector: 'app-navbar',
	imports: [
		RouterLink,
	],
	templateUrl: './navbar.component.html',
	styleUrl: './navbar.component.css',
})
export class NavbarComponent {
	private authStore = inject(AuthStore);
	protected router = inject(Router);
	
	protected readonly isLoggedIn = this.authStore.isAuthenticated;
	protected readonly isAdmin = this.authStore.isAdmin;
	protected readonly isMember = this.authStore.isMember;
	protected readonly role = this.authStore.role;
	
	protected menuVisible = signal(false);
	
	onLogout() {
		this.authStore.logout();
		this.router.navigateByUrl('').then();
	}
}
