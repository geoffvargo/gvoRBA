import { Component, inject, signal, viewChild } from '@angular/core';
import { ApiService } from '../services/api.service';
import { Room } from '../models/room.model';
import { MatCell, MatCellDef, MatColumnDef, MatHeaderCell, MatHeaderCellDef, MatHeaderRow, MatHeaderRowDef, MatRow, MatRowDef, MatTable, MatTableDataSource } from '@angular/material/table';
import { MatSort, MatSortHeader } from '@angular/material/sort';

@Component({
	selector: 'app-room-list',
	imports: [
		MatSort,
		MatTable,
		MatHeaderCell,
		MatHeaderCellDef,
		MatColumnDef,
		MatSortHeader,
		MatCell,
		MatCellDef,
		MatHeaderRow,
		MatHeaderRowDef,
		MatRow,
		MatRowDef,
	],
	templateUrl: './room-list.component.html',
	styleUrl: './room-list.component.css',
})
export class RoomListComponent {
	private apiService = inject(ApiService);
	
	_rooms = signal<Room[]>([]);
	readonly rooms = this._rooms.asReadonly();
	
	sorter = viewChild(MatSort);
	
	dataSource = new MatTableDataSource<Room>();
	displayedColumns = [
		'id',
		'name',
		'location',
		'capacity',
		'amenities',
		'isActive',
		'createdOn',
	];
	
	ngOnInit() {
		this.apiService.getRooms().subscribe({
			next: data => {
				this._rooms.set(data);
				this.dataSource.data = [...data];
			},
		});
	}
	
	ngAfterViewInit() {
		this.dataSource.sort = this.sorter();
		
		this.dataSource.sortingDataAccessor = (item: Room, property: string): any => {
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
					return item.amenities;
				case 'isActive':
					return item.isActive;
				case 'createdOn':
					return item.createdOn;
				default:
					return '';
			}
		};
	}
}
