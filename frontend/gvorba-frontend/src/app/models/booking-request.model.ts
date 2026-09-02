export interface BookingRequest {
	roomId: number;
	userId: number;
	startsAt: string;
	endsAt: string;
	purpose: string;
	status: string;
}

export class BookingRequestImpl implements BookingRequest {
	public roomId = 0;
	public userId = 0;
	public startsAt = '';
	public endsAt = '';
	public purpose = '';
	public status = '';
}
