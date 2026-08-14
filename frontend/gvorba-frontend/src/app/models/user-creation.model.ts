import { Role } from './role.model';

export interface UserCreationModel {
	name: string;
	email: string;
	password: string;
	role: Role;
	enabled: boolean;
}
