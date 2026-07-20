export interface BookingRequest {
	roomId: number;
	userId: number;
	startsAt: Date;
	endsAt: Date;
	purpose: string;
	bookingStatus: boolean
}
