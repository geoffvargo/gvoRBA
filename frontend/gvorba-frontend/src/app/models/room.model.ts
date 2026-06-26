export class Room {
	id = -1;
	name = '';
	location = '';
	capacity = -1;
	amenities: string[] = [''];
	isActive = false;
	createdOn: Date = new Date();
	
	constructor(id = -1,
	            name = '',
	            location = '',
	            capacity = -1,
	            amenities: string[] = [],
	            isActive = false,
	            createdOn: Date = new Date()) {
		this.id = id;
		this.name = name;
		this.location = location;
		this.capacity = capacity;
		this.isActive = isActive;
		this.amenities = amenities;
		this.createdOn = createdOn;
	}
}
