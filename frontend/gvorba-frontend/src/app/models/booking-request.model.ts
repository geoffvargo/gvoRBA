export interface BookingRequest {
	roomId: number;
	userId: number;
	startsAt: Date;
	endsAt: Date;
	purpose: string;
	bookingStatus: boolean
}

export class BookingRequestImpl implements BookingRequest {
	public roomId = 0;
	public userId = 0;
	public startsAt = new Date();
	public endsAt = new Date();
	public purpose = '';
	public bookingStatus: boolean = true;
}
