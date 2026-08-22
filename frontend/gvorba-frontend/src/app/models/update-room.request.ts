export interface UpdateRoomRequest {
	name: string;
	location: string;
	capacity: number;
	amenities: string[];
	isActive: boolean;
}

export class UpdateRoomRequestImpl implements UpdateRoomRequest {
	public amenities: string[] = [];
	public capacity = 0;
	public isActive = false;
	public location = '';
	public name = '';
}
