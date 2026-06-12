import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ApiService } from './api.service';

@Component({
	selector: 'app-root',
	imports: [RouterOutlet],
	templateUrl: './app.html',
	styleUrl: './app.css',
})
export class App implements OnInit {
	private apiService = inject(ApiService);
	private destroyRef = inject(DestroyRef);
	protected readonly title = signal('gvorba-frontend');
	
	status = signal('');
	timestamp = signal('');
	
	ngOnInit() {
		const subscription = this.apiService.getPing().subscribe({
			next: data => {
				this.status.set(data.status);
				this.timestamp.set(data.timestamp);
			},
		});
		
		this.destroyRef.onDestroy(() => subscription.unsubscribe());
	}
}
