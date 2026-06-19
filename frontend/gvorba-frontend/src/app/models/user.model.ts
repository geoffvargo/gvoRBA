import { Role } from './role.model';

export class User {
	id = -1;
	email = '';
	name = '';
	role: Role = new Role();
	createdOn: Date = new Date();
	
	constructor(id = -1,
	            email = '',
	            name = '',
	            role: Role = new Role(),
	            createdOn: Date = new Date()) {
		this.id = id;
		this.email = email;
		this.name = name;
		this.role = role;
		this.createdOn = createdOn;
	}
}

// export function emptyUser(overrides: Partial<User> = {}): User {
// 	return {
// 		id: 0,
// 		email: '',
// 		name: '',
// 		role: '',
// 		createdOn: new Date(),
// 		...overrides,
// 	};
// }

// export function
