import { AfterViewInit, Component, effect, inject, OnInit, viewChild, ViewEncapsulation } from '@angular/core';
import { Booking } from '../models/booking.model';
import { MatCell, MatCellDef, MatColumnDef, MatHeaderCell, MatHeaderCellDef, MatHeaderRow, MatHeaderRowDef, MatRow, MatRowDef, MatTable, MatTableDataSource } from '@angular/material/table';
import { MatSort, MatSortHeader } from '@angular/material/sort';
import { MatPaginator } from '@angular/material/paginator';
import { BookingStore } from '../stores/booking-store';

@Component({
	selector: 'app-bookings-list',
	imports: [
		MatTable,
		MatSort,
		MatSortHeader,
		MatHeaderCell,
		MatHeaderCellDef,
		MatColumnDef,
		MatCell,
		MatCellDef,
		MatHeaderRow,
		MatHeaderRowDef,
		MatRow,
		MatRowDef,
		MatPaginator,
	],
	templateUrl: './bookings-list.component.html',
	styleUrl: './bookings-list.component.css',
	encapsulation: ViewEncapsulation.None,
})
export class BookingsListComponent implements OnInit, AfterViewInit {
	protected bookingStore = inject(BookingStore);
	
	readonly bookings = this.bookingStore.myBookings;
	
	sorter = viewChild(MatSort);
	paginator = viewChild(MatPaginator);
	
	dataSource = new MatTableDataSource<Booking>();
	displayedColumns = [
		'id',
		'roomId',
		'userId',
		'startsAt',
		'endsAt',
		'cancelledAt',
		'purpose',
		'status',
		'action',
	];
	
	constructor() {
		effect(() => {
			this.dataSource.data = this.bookings();
		});
	}
	
	ngOnInit() {
		this.bookingStore.loadMyBookings();
	}
	
	ngAfterViewInit() {
		/** set the sorter  */
		this.dataSource.sort = this.sorter();
		this.dataSource.sortingDataAccessor = (item: Booking, property: string): string | number => {
			switch (property) {
				case 'id':
					return item.id;
				case 'roomId':
					return item.roomId;
				case 'userId':
					return item.userId;
				case 'startsAt':
					return item.startsAt.getTime();
				case 'endsAt':
					return item.endsAt.getTime();
				case 'cancelledAt':
					return item.cancelledAt ? item.cancelledAt.getTime() : 0;
				case 'purpose':
					return item.purpose;
				case 'status':
					return item.status;
				default:
					return '';
			}
		};
		
		if (this.paginator) {
			this.dataSource.paginator = this.paginator();
		}
	}
	
	onView(id: number, bookings: Booking) {
		console.log(id, bookings);
	}
}
