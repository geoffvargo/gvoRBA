import { Room } from './room.model';

export class UserPrefs {
	emailNotification = false;
	multifactor = false;
	confirmBookings = false;
	notifyOnAdminCancel = false;
	favoriteRooms: Room[] = [];
	
	constructor(emailNotification = false,
	            multifactor = false,
	            confirmBookings = false,
	            notifyOnAdminCancel = false,
	            favoriteRooms: Room[] = []) {
		this.emailNotification = emailNotification;
		this.multifactor = multifactor;
		this.confirmBookings = confirmBookings;
		this.notifyOnAdminCancel = notifyOnAdminCancel;
		this.favoriteRooms = favoriteRooms;
	}
}
