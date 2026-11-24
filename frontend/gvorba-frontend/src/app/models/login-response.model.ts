export interface LoginResponse {
	jwtToken: string;
	username: string;
	roles: string[];
	mfaRequired: boolean,
}

export function emptyResponse(overrides: Partial<LoginResponse> = {}): LoginResponse {
	return {
		jwtToken: '',
		username: '',
		roles: [],
		mfaRequired: false,
		...overrides,
	};
}
