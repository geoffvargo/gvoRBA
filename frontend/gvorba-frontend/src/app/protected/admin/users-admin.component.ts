import { Component, effect, inject, OnInit, viewChild, ViewEncapsulation } from '@angular/core';
import { UserStore } from '../../stores/user-store';
import { MatSort, MatSortHeader } from '@angular/material/sort';
import { MatPaginator } from '@angular/material/paginator';
import { User } from '../../models/user.model';
import { MatCell, MatCellDef, MatColumnDef, MatHeaderCell, MatHeaderCellDef, MatHeaderRow, MatHeaderRowDef, MatRow, MatRowDef, MatTable, MatTableDataSource } from '@angular/material/table';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
	selector: 'app-users-admin',
	imports: [
		MatSort,
		MatTable,
		MatColumnDef,
		MatHeaderCellDef,
		MatHeaderCell,
		MatCellDef,
		MatCell,
		MatSortHeader,
		MatHeaderRow,
		MatHeaderRowDef,
		MatRowDef,
		MatRow,
		MatPaginator,
	],
	templateUrl: './users-admin.component.html',
	styleUrl: './users-admin.component.css',
	encapsulation: ViewEncapsulation.None,
})
export class UsersAdminComponent implements OnInit {
	private router = inject(Router);
	private route = inject(ActivatedRoute);
	
	protected userStore = inject(UserStore);
	
	readonly users = this.userStore.users;
	readonly isLoading = this.userStore.isLoading;
	
	sorter = viewChild(MatSort);
	paginator = viewChild(MatPaginator);
	
	dataSource = new MatTableDataSource<User>();
	displayedColumns = [
		'id',
		'name',
		'email',
		'role',
		'created_on',
		'enabled',
		'action',
	];
	
	constructor() {
		effect(() => {
			this.dataSource.data = this.users();
			console.log(this.dataSource);
		});
		
		effect(() => {
			const sort = this.sorter();
			const paginator = this.paginator();
			
			if (sort) {
				this.dataSource.sort = sort;
			}
			
			if (paginator) {
				this.dataSource.paginator = paginator;
			}
		});
		
		this.dataSource.sortingDataAccessor = (item: User, property: string) => {
			switch (property) {
				case 'id':
					return item.id;
				case 'name':
					return item.name;
				case 'email':
					return item.email;
				case 'role':
					return item.role.roleName;
				case 'created_on':
					return item.createdOn.toISOString();
				case 'enabled':
					return item.enabled.toString();
				default:
					return '';
			}
		};
	}
	
	public ngOnInit() {
		this.userStore.loadUsers();
		console.log(this.users);
	}
	
	onView(id: string) {
		this.router.navigate([id], {
			relativeTo: this.route,
		}).then();
	}
	
	onAddUser() {
		this.router.navigate(['/admin/users/create']).then();
	}
}
