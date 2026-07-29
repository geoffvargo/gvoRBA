import { AfterViewInit, Component, effect, inject, OnInit, viewChild, ViewEncapsulation } from '@angular/core';
import { Room } from '../models/room.model';
import { MatCell, MatCellDef, MatColumnDef, MatHeaderCell, MatHeaderCellDef, MatHeaderRow, MatHeaderRowDef, MatRow, MatRowDef, MatTable, MatTableDataSource } from '@angular/material/table';
import { MatSort, MatSortHeader } from '@angular/material/sort';
import { ActivatedRoute, Router } from '@angular/router';
import { MatPaginator } from '@angular/material/paginator';
import { RoomStore } from '../stores/room-store';

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
		MatPaginator,
	],
	templateUrl: './room-list.component.html',
	styleUrl: './room-list.component.css',
	encapsulation: ViewEncapsulation.None,
})
export class RoomListComponent implements OnInit, AfterViewInit {
	private router = inject(Router);
	private route = inject(ActivatedRoute);
	
	protected roomStore = inject(RoomStore);
	
	protected readonly rooms = this.roomStore.rooms;
	
	sorter = viewChild(MatSort);
	paginator = viewChild(MatPaginator);
	
	dataSource = new MatTableDataSource<Room>();
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
	}
	
	ngOnInit() {
		this.roomStore.loadRooms();
	}
	
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
	
	onView(id: number, room: Room) {
		this.router.navigate([id], {
				relativeTo: this.route, state: { room, id },
			},
		).then();
	}
}
