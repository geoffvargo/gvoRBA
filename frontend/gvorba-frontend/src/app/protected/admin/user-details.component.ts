import { Component, effect, inject, signal } from '@angular/core';
import { UserStore } from '../../stores/user-store';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthStore } from '../../stores/auth-store';

@Component({
	selector: 'app-user-details',
	imports: [],
	templateUrl: './user-details.component.html',
	styleUrl: './user-details.component.css',
})
export class UserDetailsComponent {
	protected authStore = inject(AuthStore);
	protected userStore = inject(UserStore);
	protected router = inject(Router);
	protected activatedRoute = inject(ActivatedRoute);
	
	id = signal<number>(this.activatedRoute.snapshot.params['id']);
	
	readonly user = this.userStore.loadedUser;
	
	constructor() {
		effect(() => {
			this.userStore.loadUser(this.id());
		});
	}
}
