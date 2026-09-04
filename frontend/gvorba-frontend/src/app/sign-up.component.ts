import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthStore } from './stores/auth-store';
import { toSignal } from '@angular/core/rxjs-interop';
import { SignupRequest } from './models/signup-request.model';

@Component({
	selector: 'app-sign-up',
	imports: [
		ReactiveFormsModule,
	],
	templateUrl: './sign-up.component.html',
	styleUrl: './sign-up.component.css',
})
export class SignUpComponent {
	private router = inject(Router);
	
	private readonly fb = inject(NonNullableFormBuilder);
	
	protected authStore = inject(AuthStore);
	
	signUpForm = this.fb.group({
		username: this.fb.control('', [Validators.required]),
		email: this.fb.control('', [Validators.required]),
		password: this.fb.control('', [Validators.required]),
	});
	
	protected readonly formValue = toSignal(
		this.signUpForm.valueChanges, { initialValue: this.signUpForm.value },
	);
	
	onSubmit() {
		this.authStore.signUp(this.formValue() as SignupRequest);
		this.router.navigate(['/'], {
			replaceUrl: true,
		}).then();
	}
	
	onReset() {
		this.signUpForm.reset();
	}
}
