import { Role } from './role.model';

export class UserUpdate {
	email = '';
	name = '';
	role: Role = new Role();
	enabled = true;
}
