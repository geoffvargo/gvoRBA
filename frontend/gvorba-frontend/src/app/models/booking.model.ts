export class Booking {
	id: number = -1;
	roomId: number = -1;
	userId: number = -1;
	startsAt: Date = new Date();
	endsAt: Date = new Date();
	cancelledAt: Date = new Date();
	purpose: string = '';
	status: string = '';
	
	constructor(id: number = -1,
	            roomId: number = -1,
	            userId: number = -1,
	            startsAt: Date = new Date(),
	            endsAt: Date = new Date(),
	            cancelledAt: Date = new Date(),
	            purpose: string = '',
	            status: string = '') {
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
