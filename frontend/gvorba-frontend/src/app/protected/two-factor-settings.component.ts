import { Component, inject, signal } from '@angular/core';
import { AuthStore } from '../stores/auth-store';
import { UserStore } from '../stores/user-store';
import { HotToastService } from '@ngxpert/hot-toast';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { HttpErrorResponse } from '@angular/common/http';
import { toDataURL } from 'qrcode';
import { from, map, switchMap } from 'rxjs';

/** The screen states, in the order a user meets them. The template shows one per state. */
export type TwoFactorMode = 'loading' | 'notEnrolled' | 'setup' | 'showCodes' | 'enrolled' | 'disabling';

/**
 * The "Authenticator app" card on the settings screen (user-home). Sets up, shows and removes the
 * user's authenticator. It never changes the multifactor preference: only your prefs form does.
 */
@Component({
	selector: 'app-two-factor-settings',
	imports: [
		ReactiveFormsModule,
	],
	templateUrl: './two-factor-settings.component.html',
	styleUrl: './two-factor-settings.component.css',
})
export class TwoFactorSettingsComponent {
	private apiService = inject(ApiService);
	private authStore = inject(AuthStore);
	private userStore = inject(UserStore);
	private toast = inject(HotToastService);
	
	protected readonly mode = signal<TwoFactorMode>('loading');
	protected readonly qrDataUrl = signal('');
	protected readonly secret = signal('');
	protected readonly recoveryCodes = signal<string[]>([]);
	protected readonly multifactorPref = this.userStore.userPrefs;
	protected readonly fb = inject(NonNullableFormBuilder);
	
	protected readonly codeForm = this.fb.group({
		code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
	});
	
	constructor() {
		this.loadStatus();
	}
	
	protected loadStatus() {
		this.mode.set('loading');
		this.apiService.getTwoFactorStatus().subscribe({
			next: data => {
				this.mode.set(data.enabled ? 'enrolled' : 'notEnrolled');
			},
			error: err => {
				this.showError(err as HttpErrorResponse);
			},
		});
	}
	
	protected startSetup() {
		if (this.mode() === 'loading') {
			return;
		}
		
		this.mode.set('loading');
		this.apiService.setupTwoFactor().pipe(
			switchMap(ret => from(toDataURL(ret.otpAuthUri)).pipe(
				map(qr => ({ qr, secret: ret.secret })),
			)),
		).subscribe({
			next: ({ qr, secret }) => {
				this.qrDataUrl.set(qr);
				this.secret.set(secret);
				this.codeForm.reset();
				this.mode.set('setup');
			},
			error: err => {
				this.mode.set('notEnrolled');
				this.showError(err as HttpErrorResponse);
			},
		});
	}
	
	protected confirmSetup() {
		if (this.codeForm.invalid) {
			return;
		}
		
		this.apiService.confirmTwoFactor({ code: this.codeForm.controls.code.value }).subscribe({
			next: data => {
				this.authStore.applySession(data.session);
				this.recoveryCodes.set(data.recoveryCodes);
				this.qrDataUrl.set('');
				this.secret.set('');
				this.mode.set('showCodes');
			},
			error: err => {
				this.showError(err as HttpErrorResponse);
			},
		});
	}
	
	protected finishShowingCodes() {
		this.recoveryCodes.set([]);
		this.mode.set('enrolled');
	}
	
	protected startDisabled() {
		this.codeForm.reset();
		this.mode.set('disabling');
	}
	
	protected confirmDisabled() {
		if (this.codeForm.invalid) {
			return;
		}
		
		this.apiService.disableTwoFactor({ code: this.codeForm.controls.code.value }).subscribe({
			next: () => {
				this.toast.success('Authenticator removed successfully.', {
					position: 'bottom-center',
					dismissible: true,
				});
				this.mode.set('notEnrolled');
			},
			error: err => {
				this.showError(err as HttpErrorResponse);
			},
		});
	}
	
	protected cancel() {
		this.qrDataUrl.set('');
		this.secret.set('');
		this.codeForm.reset();
		this.loadStatus();
	}
	
	private showError(err: HttpErrorResponse) {
		this.toast.error(err.message, {
			position: 'bottom-center',
			dismissible: true,
		});
	}
}
