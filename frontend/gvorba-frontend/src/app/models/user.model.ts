import { Role } from './role.model';
import { AuthProviderEnum } from './auth-provider.enum';
import { UserPrefs } from './user.prefs';

export class User {
	id = -1;
	email = '';
	name = '';
	role: Role = new Role();
	createdOn: Date = new Date();
	enabled: boolean;
	authProvider: AuthProviderEnum;
	userPrefs = new UserPrefs();
	
	constructor(id = -1,
	            email = '',
	            name = '',
	            role: Role = new Role(),
	            createdOn: Date = new Date(),
	            isEnabled = true,
	            authProvider = AuthProviderEnum.LOCAL,
	            userprefs = new UserPrefs()) {
		this.id = id;
		this.email = email;
		this.name = name;
		this.role = role;
		this.createdOn = createdOn;
		this.enabled = isEnabled;
		this.authProvider = authProvider;
		this.userPrefs = userprefs;
	}
}
