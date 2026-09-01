export class CreateBookingRequest {
	roomId = '';
	userId = '';
	startsAt = new Date();
	endsAt = new Date();
	purpose = '';
	bookingStatus = true;
}
