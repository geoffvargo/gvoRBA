import { Role } from './role.model';

export class User {
	id = -1;
	email = '';
	name = '';
	role: Role = new Role();
	createdOn: Date = new Date();
	enabled: boolean;
	
	constructor(id = -1,
	            email = '',
	            name = '',
	            role: Role = new Role(),
	            createdOn: Date = new Date(),
	            enabled: boolean = true) {
		this.id = id;
		this.email = email;
		this.name = name;
		this.role = role;
		this.createdOn = createdOn;
		this.enabled = enabled;
	}
}
