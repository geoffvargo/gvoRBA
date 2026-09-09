import { AfterViewInit, Component, effect, inject, OnInit, viewChild, ViewEncapsulation } from '@angular/core';
import { BookingStore } from '../../stores/booking-store';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSort, MatSortHeader } from '@angular/material/sort';
import { MatPaginator } from '@angular/material/paginator';
import { MatCell, MatCellDef, MatColumnDef, MatHeaderCell, MatHeaderCellDef, MatHeaderRow, MatHeaderRowDef, MatRow, MatRowDef, MatTable, MatTableDataSource } from '@angular/material/table';
import { Booking } from '../../models/booking.model';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

@Component({
	selector: 'app-bookings-admin',
	imports: [
		MatCell,
		MatCellDef,
		MatColumnDef,
		MatHeaderCell,
		MatHeaderRow,
		MatHeaderRowDef,
		MatPaginator,
		MatRow,
		MatRowDef,
		MatSort,
		MatSortHeader,
		MatTable,
		MatHeaderCellDef,
		MatProgressSpinner,
	],
	templateUrl: './bookings-list-admin.component.html',
	styleUrl: './bookings-list-admin.component.css',
	encapsulation: ViewEncapsulation.None,
})
export class BookingsListAdminComponent implements OnInit, AfterViewInit {
	private router = inject(Router);
	private route = inject(ActivatedRoute);
	
	protected bookingStore = inject(BookingStore);
	
	readonly bookings = this.bookingStore.bookings;
	readonly isLoading = this.bookingStore.isLoading;
	
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
			
			if (this.paginator()) {
				this.dataSource.paginator = this.paginator();
			}
			if (this.sorter()) {
				this.dataSource.sort = this.sorter();
			}
		});
	}
	
	ngOnInit() {
		this.bookingStore.loadMyBookings();
		this.bookingStore.loadBookings();
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
		
		
	}
	
	onView(id: number, booking: Booking) {
		this.router.navigate(['bookings/' + id], {
			state: { booking, id },
		}).then();
	}
	
	onNewBooking() {
		this.router.navigate(['bookings/create']).then();
	}
	
}
