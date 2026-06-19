export class Role {
	roleId: number = -1;
	roleName: string = '';
	
	constructor(roleId: number = -1, roleName: string = '') {
		this.roleId = roleId;
		this.roleName = roleName;
	}
}
