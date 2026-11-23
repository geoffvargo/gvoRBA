import { LoginResponse } from './login-response.model';

/** Body of `GET /api/auth/2fa/status` (backend record StatusResponse). */
export interface StatusResponse {
	enabled: boolean;
}

/** Body of `POST /api/auth/2fa/setup` (backend record SetupResponse */
export interface SetupResponse {
	otpAuthUri: string;
	secret: string;
}

/** Body of `POST /api/auth/2fa/confirm` (backend of record ConfirmResponse) */
export interface ConfirmResponse {
	recoveryCodes: string[];
	session: LoginResponse;
}

/** Resquest body for `/confirm`, `/verify` and `/disable` (backend record OtpRequest) */
export interface OtpRequest {
	code: string;
}

/** Request body for `POST /api/auth/2fa/recover` (backend record RecoveryRequest) */
export interface RecoveryRequest {
	recoveryCode: string;
}
