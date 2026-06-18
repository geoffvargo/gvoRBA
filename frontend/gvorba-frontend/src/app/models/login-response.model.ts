export interface LoginResponse {
	jwtToken: string;
	username: string;
	roles: string[];
}

export function emptyResponse(overrides: Partial<LoginResponse> = {}): LoginResponse {
	return {
		jwtToken: '',
		username: '',
		roles: [],
		...overrides,
	};
}
