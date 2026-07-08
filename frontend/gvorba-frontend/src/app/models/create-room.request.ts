export interface CreateRoomRequest {
	name: string;
	location: string;
	capacity: number;
	description: string;
	amenities: string[];
}
