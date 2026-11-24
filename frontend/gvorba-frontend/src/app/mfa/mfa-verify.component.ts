import { Component, inject, signal } from '@angular/core';
import { AuthStore } from '../stores/auth-store';
import { Router } from '@angular/router';
import { HotToastService } from '@ngxpert/hot-toast';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { NgClass } from '@angular/common';

@Component({
	selector: 'app-mfa-verify',
	imports: [
		ReactiveFormsModule,
		NgClass,
	],
	templateUrl: './mfa-verify.component.html',
	styleUrl: './mfa-verify.component.css',
})
export class MfaVerifyComponent {
	private authStore = inject(AuthStore);
	private router = inject(Router);
	private toast = inject(HotToastService);
	
	protected readonly useRecoveryCode = signal(false);
	protected readonly isSubmitting = signal(false);
	protected readonly fb = inject(NonNullableFormBuilder);
	
	protected readonly codeForm = this.fb.group({
		code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
	});
	
	protected readonly recoveryForm = this.fb.group({
		recoveryCode: ['', Validators.required],
	});
	
	protected onSubmitCode() {
		if (this.codeForm.invalid) {
			return;
		}
		
		this.isSubmitting.set(true);
		
		this.authStore.verifyCode({ code: this.codeForm.controls.code.value }).subscribe({
			next: () => {
				this.onVerified();
			},
			error: err => {
				this.onRejected(err as HttpErrorResponse);
			},
		});
	}
	
	protected onSuibmitRecoveryCode() {
		if (this.recoveryForm.invalid) {
			return;
		}
		
		this.isSubmitting.set(true);
		
		this.authStore.recoverWithCode(this.recoveryForm.controls.recoveryCode.value).subscribe({
			next: () => {
				this.onVerified();
			},
			error: err => {
				this.onRejected(err as HttpErrorResponse);
			},
		});
	}
	
	protected toggleMode() {
		this.useRecoveryCode.update(state => !state);
	}
	
	protected onCancel() {
		this.authStore.logout();
		this.router.navigate(['']).then();
	}
	
	private onVerified() {
		this.isSubmitting.set(false);
		this.router.navigate(['']).then();
	}
	
	protected onRejected(err: HttpErrorResponse) {
		this.isSubmitting.set(false);
		this.toast.error(err.message, {
			position: 'bottom-center',
			dismissible: true,
		});
	}
}
