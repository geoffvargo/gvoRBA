import { Room } from './room.model';
import { User } from './user.model';

export class BookingResponse {
	id = -1;
	room = new Room();
	userId = new User();
	startsAt: Date = new Date();
	endsAt: Date = new Date();
	cancelledAt: Date | null = null;
	purpose = '';
	status = '';
	
	constructor(id = -1,
	            room = new Room(),
	            userId = new User(),
	            startsAt: Date = new Date(),
	            endsAt: Date = new Date(),
	            cancelledAt: Date | null = null,
	            purpose = '',
	            status = '') {
		this.id = id;
		this.room = room;
		this.userId = userId;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.cancelledAt = cancelledAt;
		this.purpose = purpose;
		this.status = status;
	}
}
