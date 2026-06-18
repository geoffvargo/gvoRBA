import { Component, inject, signal, ViewEncapsulation } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { LoginRequest } from '../models/login-request.model';
import { emptyResponse, LoginResponse } from '../models/login-response.model';

@Component({
	selector: 'app-landing-page',
	imports: [RouterLink, ReactiveFormsModule],
	templateUrl: './landing-page.html',
	styleUrl: './landing-page.css',
	encapsulation: ViewEncapsulation.None,
})
export class LandingPage {
	private apiService = inject(ApiService);
	
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
				sessionStorage.setItem('auth-token', response.jwtToken);
			},
			error: err => console.log(err),
		});
	}
}
