import { Component, computed, effect, inject, OnInit, Signal, signal, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { BookingStore } from '../stores/booking-store';
import { AuthStore } from '../stores/auth-store';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatDatepicker, MatDatepickerInput, MatDatepickerToggle } from '@angular/material/datepicker';
import { MatFormField, MatInput, MatSuffix } from '@angular/material/input';
import { MatOption, provideNativeDateAdapter } from '@angular/material/core';
import { MatSelect } from '@angular/material/select';
import { addDays, combineDateAndMinutes, DAY_START, DEFAULT_DUR, HORIZON_DAYS, isWeekday, maxDurationFor, MIN_DUR, START_OPTIONS, startOfToday, toDateTimeString } from '../utils/booking-time';
import { toSignal } from '@angular/core/rxjs-interop';
import { UserStore } from '../stores/user-store';
import { BookingRequest } from '../models/booking-request.model';

@Component({
	selector: 'app-booking-details',
	imports: [
		MatProgressSpinner,
		ReactiveFormsModule,
		MatDatepicker,
		MatDatepickerInput,
		MatDatepickerToggle,
		MatFormField,
		MatInput,
		MatOption,
		MatSelect,
		MatSuffix,
	],
	providers: [provideNativeDateAdapter()], // the datepicker works with plain JS Dates
	templateUrl: './booking-details.component.html',
	styleUrl: './booking-details.component.css',
	encapsulation: ViewEncapsulation.None,
})
export class BookingDetailsComponent implements OnInit {
	private router = inject(Router);
	private route = inject(ActivatedRoute);
	private location = inject(Location);
	
	private readonly fb = inject(NonNullableFormBuilder);
	
	protected bookingStore = inject(BookingStore);
	protected authStore = inject(AuthStore);
	protected userStore = inject(UserStore);
	
	protected bookingId = signal<number>(this.route.snapshot.params['id']);
	protected users = this.userStore.users;
	protected booking = this.bookingStore.currentBooking;
	protected isLoading = this.bookingStore.isLoading;
	protected isAdmin = this.authStore.isAdmin;
	
	isEditing = signal(false);
	
	readonly currUser = this.authStore.user;
	readonly isCancelled = this.bookingStore.isCancelled;
	
	readonly isOwner = computed(() =>
		this.isAdmin() || this.currUser()?.id === this.booking().userId.id);
	readonly attendeeNames = computed(() => {
		return this.booking().attendees.map(attendee => attendee.name).join(', ');
	});
	
	protected readonly isWeekdayFilter = isWeekday;
	protected readonly startOptions = signal(START_OPTIONS);
	protected readonly MIN_DUR = MIN_DUR; // exposed for the template's [min] binding
	
	editingForm = this.fb.group({
		date: this.fb.control<Date | null>(null),
		startsAt: this.fb.control<number | null>(null),
		duration: this.fb.control<number | null>(null),
		purpose: this.fb.control(''),
		attendees: this.fb.control<number[]>([]),
	});
	
	/** Signal versions of the form's values, so computed()s can react to edits. */
	protected readonly formValue = toSignal(
		this.editingForm.valueChanges, {
			initialValue: this.editingForm.value,
		},
	);
	
	/** Builds the request body from the form, or null while required fields are still missing. */
	private readonly bookingPayload = computed((): BookingRequest | null => {
		
		const { date, purpose, attendees } = this.formValue();
		const start = this.startMinutes();
		
		if (!date || start == null || !purpose || !attendees) {
			console.log('[bookingPayload] guard failed on:', {
				date: !date, start: start == null, purpose: !purpose, attendees: !attendees,
			});
			return null;
		}
		
		console.log('this.endMinutes(): ', this.endMinutes());
		
		const startsAt: string = toDateTimeString(combineDateAndMinutes(date, start));
		const endsAt: string = toDateTimeString(combineDateAndMinutes(date, this.endMinutes()));
		
		return {
			roomId: this.booking().room.id,
			userId: this.booking().userId.id,
			startsAt: startsAt,
			endsAt: endsAt,
			purpose,
			status: this.booking().status,
			attendees,
		} as BookingRequest;
	});
	
	durationMinutes: Signal<number | null> = toSignal(this.editingForm.controls.duration.valueChanges, {
		initialValue: this.editingForm.controls.duration.value,
	});
	
	// Selected start time in minutes, as a signal.
	startMinutes: Signal<number | null> = toSignal(this.editingForm.controls.startsAt.valueChanges, {
		initialValue: this.editingForm.controls.startsAt.value,
	});
	
	// Datepicker bounds: today through HORIZON_DAYS ahead.
	today = signal(startOfToday());
	minDate = computed(() => this.today());
	maxDate = computed(() => addDays(this.today(), HORIZON_DAYS));
	
	// Duration cap for the chosen start time, and the resulting end time (both in minutes).
	maxDuration = computed(() => maxDurationFor(this.startMinutes() ?? DAY_START));
	endMinutes = computed(() => (this.startMinutes() ?? DAY_START) + (this.durationMinutes() ?? DEFAULT_DUR));
	
	constructor() {
		effect(() => {
			this.populateForm();
		});
	}
	
	ngOnInit() {
		this.bookingStore.loadBooking(this.bookingId());
		console.log('currentBooking', this.booking());
		console.log(this.attendeeNames());
	}
	
	onCancelBooking() {
		this.bookingStore.cancelBooking(this.bookingId());
		this.bookingStore.loadBookings();
		this.onBack();
	}
	
	onUncancelBooking() {
		this.bookingStore.uncancelBooking(this.bookingId());
		this.onBack();
	}
	
	onExportIcs() {
		console.log('exportIcs()');
		this.bookingStore.exportIcs(this.bookingId());
	}
	
	onEdit() {
		console.log('edit');
		this.isEditing.set(true);
	}
	
	populateForm() {
		const booking = this.booking();
		
		if (booking == null) {
			return;
		}
		
		const attendeeList = booking.attendees.map(attendee => attendee.id) ?? null;
		
		const durationValue = (new Date(booking.endsAt).getTime() - new Date(booking.startsAt).getTime()) / 60_000;
		
		const startsAtValue = new Date(booking.startsAt).getHours() * 60 + new Date(booking.startsAt).getMinutes();
		
		this.editingForm.patchValue({
			date: booking.startsAt,
			startsAt: startsAtValue,
			duration: durationValue,
			purpose: booking.purpose,
			attendees: attendeeList,
		});
	}
	
	onSave() {
		const payload = this.bookingPayload();
		
		if (!payload) {
			return;
		}
		
		this.bookingStore.updateBooking(this.bookingId(), payload);
		
		this.editingForm.reset();
		this.isEditing.set(false);
	}
	
	onReset() {
		console.log('reset');
		this.editingForm.reset();
	}
	
	onCancel() {
		console.log('cancel');
		this.editingForm.reset();
		this.isEditing.set(false);
	}
	
	onBack() {
		this.location.back();
	}
}
