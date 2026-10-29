import { computed, inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../services/api.service';
import { User } from '../models/user.model';
import { Role } from '../models/role.model';
import { UserUpdate } from '../models/user-update.model';
import { UserCreationModel } from '../models/user-creation.model';
import { firstValueFrom, Observable, tap } from 'rxjs';
import { HotToastService } from '@ngxpert/hot-toast';
import { AuthProviderEnum } from '../models/auth-provider.enum';
import { UserPrefs } from '../models/user.prefs';

@Injectable({
	providedIn: 'root',
})
export class UserStore {
	private apiService = inject(ApiService);
	private toast = inject(HotToastService);
	
	private _users = signal<User[]>([]);
	private _isLoading = signal<boolean>(false);
	private _loadedUser = signal<User | null>(null);
	private _isLocal = signal<boolean>(false);
	private _userPrefs = signal<UserPrefs>(new UserPrefs());
	
	readonly users = this._users.asReadonly();
	readonly isLoading = this._isLoading.asReadonly();
	readonly loadedUser = this._loadedUser.asReadonly();
	readonly isLocal = this._isLocal.asReadonly();
	readonly userPrefs = this._userPrefs.asReadonly();
	
	/// The logged-in user. AuthStore drives this through loadCurrentUser()/clearCurrentUser()
	/// on login, refresh, and logout.
	private _user = signal<User | null>(null);
	readonly user = this._user.asReadonly();
	
	/// `.role` can itself be null/undefined even when `_user` isn't, so both hops
	/// need `?.` — otherwise a user payload with a missing role throws here
	/// instead of falling back to ROLE_GUEST.
	readonly role = computed(() => this._user()?.role?.roleName ?? 'ROLE_GUEST');
	readonly isAdmin = computed(() => this.role() === 'ROLE_ADMIN');
	readonly isMember = computed(() => this.role() === 'ROLE_MEMBER');
	
	/**
	 * Fetches the logged-in user from /api/auth/me and stores it in {@link user}.
	 * Returns a cold Observable — nothing is fetched until it's subscribed — so
	 * AuthStore can chain it into its login/refresh pipelines.
	 *
	 * No loadUsers() in the constructor: AuthStore injects this store at app start,
	 * so an eager call would hit the admin-only /api/users/ endpoint for every visitor.
	 * Components that need the user list call loadUsers() themselves.
	 */
	loadCurrentUser(): Observable<User> {
		return this.apiService.getCurrentUser().pipe(
			tap(user => {
				this._user.set(user);
				this._userPrefs.set(user.userPrefs);
				console.log('userPrefs: ', this._userPrefs());
			}),
		);
	}
	
	/** Clears the logged-in user (logout / failed session restore). */
	clearCurrentUser() {
		this._user.set(null);
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
				this._isLocal.set(this._loadedUser()?.authProvider === AuthProviderEnum.LOCAL);
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
	
	getPrefs(id: number) {
		this._isLoading.set(true);
		this.apiService.getUserPrefs(id).subscribe({
			next: data => {
				this._userPrefs.set(data);
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
	
	setPrefs(id: number, payload: UserPrefs) {
		this._isLoading.set(true);
		this.apiService.setUserPrefs(id, payload).subscribe({
			next: data => {
				console.log(data);
				this.toast.success('User set prefs successfully.', {
					position: 'bottom-center',
					dismissible: true,
				});
				/// Reload only after the save lands, so the GET can't race ahead and return stale prefs.
				this.getPrefs(id);
			},
			error: err => {
				console.error(err);
				this.toast.error(err.message, {
					position: 'bottom-center',
					dismissible: true,
				});
				this._isLoading.set(false);
			},
		});
	}
}
