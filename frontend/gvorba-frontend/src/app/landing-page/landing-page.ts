import { Component, effect, inject, signal, ViewEncapsulation } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LoginRequest } from '../models/login-request.model';
import { emptyResponse, LoginResponse } from '../models/login-response.model';
import { AuthStore } from '../stores/auth-store';

@Component({
	selector: 'app-landing-page',
	imports: [RouterLink, ReactiveFormsModule],
	templateUrl: './landing-page.html',
	styleUrl: './landing-page.css',
	encapsulation: ViewEncapsulation.None,
})
export class LandingPage {
	private router = inject(Router);
	private authStore = inject(AuthStore);
	
	loginRequest = signal<LoginRequest>({ username: '', password: '' });
	loginResponse = signal<LoginResponse>(emptyResponse());
	
	loginForm = new FormGroup({
		username: new FormControl('', [Validators.required]),
		password: new FormControl('', [Validators.required]),
	});
	
	constructor() {
		this.loginForm.valueChanges.subscribe(value => {
			this.loginRequest.update(curr => ({
				...curr,
				username: value.username ?? '',
				password: value.password ?? '',
			}));
		});
		
		effect(() => {
			if (this.authStore.isAuthenticated()) {
				this.router.navigate(['/home']).then();
			}
		});
	}
	
	onSubmit() {
		if (this.loginForm.invalid) {
			return;
		}
		
		this.authStore.login(this.loginRequest()).subscribe({
			next: () => {
				this.router.navigate(['/home']).then();
			},
			error: err => {
				console.error(err);
			},
		});
	}
}
