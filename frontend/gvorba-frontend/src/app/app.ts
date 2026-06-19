import { Component, DestroyRef, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ApiService } from './services/api.service';

@Component({
	selector: 'app-root',
	imports: [RouterOutlet],
	templateUrl: './app.html',
	styleUrl: './app.css',
})
export class App {
	private apiService = inject(ApiService);
	private destroyRef = inject(DestroyRef);
	protected readonly title = signal('gvorba-frontend');
}
