import { Component, inject, OnInit, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthStore } from '../stores/auth-store';

@Component({
	selector: 'app-oauth2-callback.component',
	imports: [],
	templateUrl: './oauth2-callback.component.html',
	styleUrl: './oauth2-callback.component.css',
	encapsulation: ViewEncapsulation.None,
})
export class Oauth2CallbackComponent implements OnInit {
	private router = inject(Router);
	private readonly route = inject(ActivatedRoute);
	
	protected authStore = inject(AuthStore);
	
	ngOnInit() {
		const error = this.route.snapshot.queryParamMap.get('error');
		
		if (error) {
			this.router.navigate([''], {
				queryParams: { error },
			}).then();
			console.log(error);
		}
		
		this.authStore.refresh().subscribe({
			next: () => {
				this.router.navigate(['home']).then();
			},
			error: error => {
				console.error(error);
				this.router.navigate(['']).then();
			}
		});
	}
}
