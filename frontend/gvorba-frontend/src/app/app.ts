import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ApiService } from './services/api.service';

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
	
	ngOnInit() {
	}
}
