import { Room } from './room.model';

export class UserPrefs {
	emailNotification = false;
	multifactor = false;
	confirmBookings = false;
	notifyOnAdminCancel = false;
	notifyOnBookingChange = false;
	notifyOnRoomDeactivated = false;
	favoriteRooms: Room[] = [];
	
	constructor(emailNotification = false,
	            multifactor = false,
	            confirmBookings = false,
	            notifyOnAdminCancel = false,
	            notifyOnBookingChange = false,
	            notifyOnRoomDeactivated = false,
	            favoriteRooms: Room[] = []) {
		this.emailNotification = emailNotification;
		this.multifactor = multifactor;
		this.confirmBookings = confirmBookings;
		this.notifyOnAdminCancel = notifyOnAdminCancel;
		this.notifyOnBookingChange = notifyOnBookingChange;
		this.notifyOnRoomDeactivated = notifyOnRoomDeactivated;
		this.favoriteRooms = favoriteRooms;
	}
}
