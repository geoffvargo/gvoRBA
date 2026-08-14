import { AfterViewInit, Component, effect, inject, viewChild } from '@angular/core';
import { RoomStore } from '../../stores/room-store';
import { ActivatedRoute, Router } from '@angular/router';
import { Room } from '../../models/room.model';
import { MatTableDataSource } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';
import { MatPaginator } from '@angular/material/paginator';

@Component({
	selector: 'app-rooms-admin',
	imports: [],
	templateUrl: './rooms-admin.component.html',
	styleUrl: './rooms-admin.component.css',
})
export class RoomsAdminComponent implements AfterViewInit {
	private router = inject(Router);
	private route = inject(ActivatedRoute);
	
	protected roomStore = inject(RoomStore);
	
	readonly rooms = this.roomStore.rooms;
	readonly isLoading = this.roomStore.isLoading;
	
	dataSource = new MatTableDataSource<Room>();
	
	constructor() {
		effect(() => {
			this.roomStore.loadRooms();
			this.dataSource.data = this.rooms();
		});
	}
	
	sorter = viewChild(MatSort);
	paginator = viewChild(MatPaginator);
	
	displayedColumns = [
		'id',
		'name',
		'location',
		'capacity',
		'amenities',
		'isActive',
		'createdOn',
		'action',
	];
	
	ngAfterViewInit() {
		this.dataSource.sort = this.sorter();
		
		this.dataSource.sortingDataAccessor = (item: Room, property: string): string | number => {
			switch (property) {
				case 'id':
					return item.id;
				case 'name':
					return item.name;
				case 'location':
					return item.location;
				case 'capacity':
					return item.capacity;
				case 'amenities':
					return item.amenities.join(', ');
				case 'isActive':
					return item.isActive ? 1 : 0;
				case 'createdOn':
					return item.createdOn.getTime();
				default:
					return '';
			}
		};
		
		if (this.paginator) {
			this.dataSource.paginator = this.paginator();
		}
	}
}
