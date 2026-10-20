import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthStore } from '../stores/auth-store';
import { UserStore } from '../stores/user-store';
import { NgClass } from '@angular/common';

@Component({
	selector: 'app-navbar',
	imports: [
		RouterLink,
		NgClass,
	],
	templateUrl: './navbar.component.html',
	styleUrl: './navbar.component.css',
})
export class NavbarComponent {
	private authStore = inject(AuthStore);
	private userStore = inject(UserStore);
	protected router = inject(Router);
	
	protected readonly isLoggedIn = this.authStore.isAuthenticated;
	protected readonly isAdmin = this.userStore.isAdmin;
	protected readonly role = this.userStore.role;
	
	menuVisible = signal(false);
	
	onLogout() {
		this.menuVisible.set(false);
		this.authStore.logout();
		this.router.navigateByUrl('').then();
	}
	
	toggleMenu() {
		this.menuVisible.update(state => !state);
	}
}
