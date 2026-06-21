export class Room {
	id: number = -1;
	name: string = '';
	location: string = '';
	capacity: number = -1;
	amenities: string[] = [''];
	isActive: boolean = false;
	createdOn: Date = new Date();
	
	constructor(id: number = -1,
	            name: string = '',
	            location: string = '',
	            capacity: number = -1,
	            amenities: string[] = [],
	            isActive: boolean = false,
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
