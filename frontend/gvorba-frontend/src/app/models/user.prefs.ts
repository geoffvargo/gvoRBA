export class UserPrefs {
	emailNotification = false;
	multifactor = false;
	confirmBookings = false;
	notifyOnAdminCancel = false;
	favoriteRooms = [];
	
	constructor(emailNotification = false,
	            multifactor = false,
	            confirmBookings = false,
	            notifyOnAdminCancel = false,
	            favoriteRooms = []) {
		this.emailNotification = emailNotification;
		this.multifactor = multifactor;
		this.confirmBookings = confirmBookings;
		this.notifyOnAdminCancel = notifyOnAdminCancel;
		this.favoriteRooms = favoriteRooms;
	}
}
