import { Routes } from '@angular/router';
import { LandingPage } from './landing-page/landing-page';
import { AccessDenied } from './access-denied/access-denied';
import { UserHomeComponent } from './protected/user-home.component';
import { authGuard } from './guards/auth-guard';

export const routes: Routes = [
	{ path: '', component: LandingPage },
	{ path: 'access-denied', component: AccessDenied },
	{ path: 'home', component: UserHomeComponent, canActivate: [authGuard] },
];
