export class Booking {
	id = -1;
	roomId = -1;
	userId = -1;
	startsAt: Date = new Date();
	endsAt: Date = new Date();
	cancelledAt: Date | null = null;
	purpose = '';
	status = '';
	
	constructor(id = -1,
	            roomId = -1,
	            userId = -1,
	            startsAt: Date = new Date(),
	            endsAt: Date = new Date(),
	            cancelledAt: Date | null = null,
	            purpose = '',
	            status = '') {
		this.id = id;
		this.roomId = roomId;
		this.userId = userId;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.cancelledAt = cancelledAt;
		this.purpose = purpose;
		this.status = status;
	}
}
