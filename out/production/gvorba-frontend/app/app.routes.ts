import { Routes } from '@angular/router';
import { LandingPage } from './landing-page/landing-page';
import { AccessDenied } from './access-denied/access-denied';
import { UserHomeComponent } from './protected/user-home.component';
import { authGuard } from './guards/auth-guard';
import { RoomListComponent } from './protected/room-list.component';
import { BookingsListComponent } from './protected/bookings-list.component';
import { RoomDetailsComponent } from './protected/room-details.component';
import { UsersAdminComponent } from './protected/admin/users-admin.component';
import { adminGuard } from './guards/admin-guard';
import { UserAdminDetailsComponent } from './protected/admin/user-admin-details.component';
import { AdminAddUserComponent } from './protected/admin/admin-add-user.component';
import { RoomsAdminComponent } from './protected/admin/rooms-admin.component';
import { RoomsCreateComponent } from './protected/admin/rooms-create.component';

export const routes: Routes = [
	{ path: '', component: LandingPage },
	{ path: 'access-denied', component: AccessDenied },
	{ path: 'home', component: UserHomeComponent, canActivate: [authGuard] },
	{ path: 'admin/rooms', component: RoomsAdminComponent, canActivate: [adminGuard] },
	{ path: 'admin/rooms/create', component: RoomsCreateComponent, canActivate: [adminGuard] },
	{ path: 'admin/users', component: UsersAdminComponent, canActivate: [adminGuard] },
	{ path: 'admin/rooms', component: RoomListComponent, canActivate: [adminGuard] },
	{ path: 'admin/bookings', component: BookingsListComponent, canActivate: [adminGuard] },
	{ path: 'admin/users/create', component: AdminAddUserComponent, canActivate: [adminGuard] },
	{ path: 'admin/users/:id', component: UserAdminDetailsComponent, canActivate: [adminGuard] },
	{ path: 'rooms', component: RoomListComponent, canActivate: [authGuard] },
	{ path: 'rooms/:id', component: RoomDetailsComponent, canActivate: [authGuard] },
	{ path: 'bookings', component: BookingsListComponent, canActivate: [authGuard] },
];
