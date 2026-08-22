import { Component, effect, inject, OnInit, viewChild, ViewEncapsulation } from '@angular/core';
import { RoomStore } from '../../stores/room-store';
import { ActivatedRoute, Router } from '@angular/router';
import { Room } from '../../models/room.model';
import { MatCell, MatCellDef, MatColumnDef, MatHeaderCell, MatHeaderCellDef, MatHeaderRow, MatHeaderRowDef, MatRow, MatRowDef, MatTable, MatTableDataSource } from '@angular/material/table';
import { MatSort, MatSortHeader } from '@angular/material/sort';
import { MatPaginator } from '@angular/material/paginator';

@Component({
	selector: 'app-rooms-admin',
	imports: [
		MatSort,
		MatTable,
		MatColumnDef,
		MatHeaderCell,
		MatHeaderCellDef,
		MatCell,
		MatCellDef,
		MatSortHeader,
		MatHeaderRow,
		MatHeaderRowDef,
		MatRow,
		MatRowDef,
		MatPaginator,
	],
	templateUrl: './rooms-admin.component.html',
	styleUrl: './rooms-admin.component.css',
	encapsulation: ViewEncapsulation.None,
})
export class RoomsAdminComponent implements OnInit {
	private router = inject(Router);
	private route = inject(ActivatedRoute);
	
	protected roomStore = inject(RoomStore);
	
	readonly rooms = this.roomStore.rooms;
	readonly isLoading = this.roomStore.isLoading;
	
	dataSource = new MatTableDataSource<Room>();
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
	
	constructor() {
		effect(() => {
			this.dataSource.data = this.rooms();
		});
		
		effect(() => {
			this.dataSource.sort = this.sorter();
			if (this.paginator) {
				this.dataSource.paginator = this.paginator();
			}
		});
		
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
	}
	
	ngOnInit() {
		/* necessary to make sure that isLoading flips to false in time so that our list populates reliably */
		this.roomStore.loadRooms().subscribe();
		console.log(this.roomStore.rooms());
	}
	
	onManage(id: number, room: Room) {
		this.router.navigate([id], {
			relativeTo: this.route,
			state: { room, id },
		}).then();
	}
	
	onAddRoom() {
		this.router.navigate(['/admin/rooms/create']).then();
	}
}
