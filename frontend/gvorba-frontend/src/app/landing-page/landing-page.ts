import { Component, inject, signal, ViewEncapsulation } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { LoginRequest } from '../models/login-request.model';
import { emptyResponse, LoginResponse } from '../models/login-response.model';
import { TokenStorageService } from '../services/token-storage-service';

@Component({
	selector: 'app-landing-page',
	imports: [RouterLink, ReactiveFormsModule],
	templateUrl: './landing-page.html',
	styleUrl: './landing-page.css',
	encapsulation: ViewEncapsulation.None,
})
export class LandingPage {
	private apiService = inject(ApiService);
	private tokenStorageService = inject(TokenStorageService);
	private router = inject(Router);
	
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
	}
	
	onSubmit() {
		this.apiService.loginUser(this.loginRequest()).subscribe({
			next: (response: LoginResponse) => {
				console.log('logged in: ', response);
				this.loginResponse.set(response);
				this.tokenStorageService.saveToken(response.jwtToken);
				this.apiService.notifyLoggedIn();
				this.router.navigate(['/home']);
			},
			error: err => console.log(err),
		});
	}
}
