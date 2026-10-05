import { Component, computed, effect, inject, signal, Signal, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AbstractControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { RoomStore } from '../stores/room-store';
import { UserStore } from '../stores/user-store';
import { MatDatepicker, MatDatepickerInput, MatDatepickerToggle } from '@angular/material/datepicker';
import { MatFormField, MatInput, MatSuffix } from '@angular/material/input';
import { MatOption, provideNativeDateAdapter } from '@angular/material/core';
import { MatSelect } from '@angular/material/select';
import { BookingStore } from '../stores/booking-store';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { BookingRequest } from '../models/booking-request.model';
import { formatDate } from '@angular/common';
import { AuthStore } from '../stores/auth-store';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

// All times below are minutes since midnight.
const DAY_START = 480;    // 08:00  (FR-4.3)
const DAY_END = 1080;     // 18:00  (FR-4.3)
const STEP = 15;          // FR-3.1 slot quantum
const MIN_DUR = 15;       // FR-3.1
const MAX_DUR = 240;      // FR-3.1 (4h)
const DEFAULT_DUR = 60;   // used for the end time until a duration is entered
const HORIZON_DAYS = 30;  // FR-3.1: how far ahead a booking can be made

/** Returns [from, from + step, ...] up to but not including `to`. */
const timerange = (from: number, to: number, step: number) => {
	const ans = [];
	for (let i = from; i < to; i += step) {
		ans.push(i);
	}
	return ans;
};

/** Zero-pads a number to two digits (7 -> "07"). */
const pad2 = (num: number) => {
	return String(num).padStart(2, '0');
};

/** Formats minutes since midnight as "HH:mm" (510 -> "08:30"). */
const timeLabel = (minutes: number) => {
	const h = Math.floor(minutes / 60);
	const m = minutes % 60;
	
	const hh: string = pad2(h);
	const mm: string = pad2(m);
	const str: string = hh + ':' + mm;
	console.log(str);
	return str;
};

// Every allowed start time in the working day, in STEP increments.
const START_VALUES = timerange(DAY_START, DAY_END, STEP);

// Start times as { value, label } pairs for the start-time <mat-select>.
export const START_OPTIONS: { value: number, label: string }[] = START_VALUES.map(v => ({
	value: v,
	label: timeLabel(v),
}));

/** Returns a copy of date with its time-of-day set to the given minutes since midnight. */
const combineDateAndMinutes = (date: Date, minutes: number): Date => {
	const combined = new Date(date);
	combined.setHours(Math.floor(minutes / 60), minutes % 60, 0, 0);
	return combined;
};

/** Returns value when it lies within the inclusive bounds, otherwise returns whichever bound it exceeded. */
const clamp = (value: number, min: number, max: number) => {
	return (value < min) ? min : (value > max) ? max : value;
};

/** Longest duration allowed for a given start time: MAX_DUR, or less if that would run past DAY_END. */
const maxDurationFor = (minutes: number) => {
	return Math.min(MAX_DUR, DAY_END - minutes);
};

/** True for Monday-Friday; null means today. Also used as the datepicker's date filter. */
export const isWeekday = (d: Date | null): boolean => {
	const day = (d ?? new Date()).getDay();
	return day !== 0 && day !== 6;   // 0 = Sunday, 6 = Saturday
};

/** Returns the first weekday strictly after `from`. */
export const nextWeekdayFrom = (from: Date) => {
	const day = new Date(new Date(from).setDate(from.getDate() + 1));
	while (!isWeekday(day)) {
		day.setDate(day.getDate() + 1);
	}
	
	return day;
};

/** Today at 00:00 local time. */
const startOfToday = () => {
	return new Date(new Date().setHours(0, 0, 0, 0));
};

/** Returns a copy of date moved forward by `days`. */
const addDays = (date: Date, days: number) => {
	const copy = new Date(date);
	copy.setDate(copy.getDate() + days);
	return copy;
};

/** True when both dates fall on the same calendar day in local time, ignoring the time of day. */
export const isSameLocalDay = (a: Date, b: Date) => {
	return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
};

/** Formats as "yyyy-M-d" in local time (no zero-padding). */
export const formatLocalDate = (date: Date) => {
	return date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate();
};

/** Formats as a zone-less "yyyy-MM-ddTHH:mm:ss" string, which the backend parses as a LocalDateTime. */
export const toDateTimeString = (date: Date) => {
	return formatDate(date, 'yyyy-MM-ddTHH:mm:ss', 'en-US');
};

@Component({
	selector: 'app-create-booking',
	imports: [
		ReactiveFormsModule,
		MatFormField,
		MatInput,
		MatDatepickerInput,
		MatDatepicker,
		MatSuffix,
		MatDatepickerToggle,
		MatSelect,
		MatOption,
		MatSlideToggle,
		MatProgressSpinner,
	],
	providers: [provideNativeDateAdapter()], // the datepicker works with plain JS Dates
	templateUrl: './create-booking.component.html',
	styleUrl: './create-booking.component.css',
	encapsulation: ViewEncapsulation.None,
})
export class CreateBookingComponent {
	private router = inject(Router);
	private route = inject(ActivatedRoute);
	
	private readonly fb = inject(NonNullableFormBuilder);
	
	protected roomStore = inject(RoomStore);
	protected userStore = inject(UserStore);
	protected bookingStore = inject(BookingStore);
	protected authStore = inject(AuthStore);
	
	// Show the spinner until every store the form depends on has loaded.
	protected readonly isLoading = computed(() =>
		this.roomStore.isLoading() || this.userStore.isLoading() || this.authStore.isLoading(),
	);
	protected readonly rooms = this.roomStore.rooms;
	protected readonly users = this.userStore.users;
	protected readonly isAdmin = this.authStore.isAdmin;
	protected readonly currentUser = this.authStore.user;
	protected readonly startOptions = signal(START_OPTIONS);
	protected readonly isWeekdayFilter = isWeekday;
	protected readonly MIN_DUR = MIN_DUR; // exposed for the template's [min] binding

	/** Form-level validator: flags a booking whose start + duration runs past DAY_END. */
	private readonly endWithinWorkingHours: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
		const group = control as FormGroup<{
			duration: AbstractControl<number | null>;
			startsAt: AbstractControl<number | null>;
		}>;

		const { startsAt: start, duration } = group.getRawValue();
		
		if (start == null || duration == null) {
			return null;
		}
		
		const end = start + duration;
		
		return end > DAY_END ? { endAfterClass: { end, limit: DAY_END, overBy: end - DAY_END } } : null;
	};
	
	// startsAt and duration are in minutes; date is the day only.
	bookingCreateForm = this.fb.group({
			roomId: this.fb.control<number>(0, [Validators.required]),
			userId: this.fb.control<number>(this.currentUser()?.id ?? 0, [Validators.required]),
			date: this.fb.control<Date | null>(null, [Validators.required]),
			startsAt: this.fb.control<number | null>(null, [Validators.required]),
			duration: this.fb.control<number | null>(null, [Validators.required]),
			purpose: this.fb.control('', [Validators.required]),
			bookingStatus: this.fb.control(true, [Validators.required]),
			attendees: this.fb.control<number[]>([]),
		}, {
			validators: [this.endWithinWorkingHours],
		},
	);
	
	/** Builds the request body from the form, or null while required fields are still missing. */
	private readonly bookingPayload = computed((): BookingRequest | null => {
		console.log(this.bookingCreateForm.controls.roomId.value);
		
		const { roomId, userId, date, purpose, bookingStatus, attendees } = this.formValue();
		const start = this.startMinutes();
		
		if (!roomId || !userId || !date || start == null || !purpose || !attendees) {
			console.log('[bookingPayload] guard failed on:', {
				roomId: !roomId, userId: !userId, date: !date, start: start == null, purpose: !purpose, attendees: !attendees,
			});
			return null;
		}
		
		console.log('this.endMinutes(): ', this.endMinutes());
		
		const startsAt: string = toDateTimeString(combineDateAndMinutes(date, start));
		const endsAt: string = toDateTimeString(combineDateAndMinutes(date, this.endMinutes()));
		
		return {
			roomId,
			userId,
			startsAt: startsAt,
			endsAt: endsAt,
			purpose,
			status: bookingStatus ? 'CONFIRMED' : 'CANCELLED',
			attendees,
		} as BookingRequest;
	});
	
	// Signal versions of the form's values, so computed()s can react to edits.
	protected readonly formValue = toSignal(
		this.bookingCreateForm.valueChanges, {
			initialValue: this.bookingCreateForm.value,
		},
	);
	
	// Datepicker bounds: today through HORIZON_DAYS ahead.
	today = signal(startOfToday());
	minDate = computed(() => this.today());
	maxDate = computed(() => addDays(this.today(), HORIZON_DAYS));
	// Duration cap for the chosen start time, and the resulting end time (both in minutes).
	maxDuration = computed(() => maxDurationFor(this.startMinutes() ?? DAY_START));
	endMinutes = computed(() => (this.startMinutes() ?? DAY_START) + (this.durationMinutes() ?? DEFAULT_DUR));
	
	durationMinutes: Signal<number | null> = toSignal(this.bookingCreateForm.controls.duration.valueChanges, {
		initialValue: this.bookingCreateForm.controls.duration.value,
	});
	
	constructor() {
		this.authStore.loadCurrentUser();

		// Non-admins can only book for themselves: pin userId to the logged-in user once it loads.
		effect(() => {
			if (!this.isAdmin()) {
				const currentUserId = this.authStore.user()?.id;
				if (currentUserId != null) {
					this.bookingCreateForm.controls.userId.setValue(currentUserId);
				}
			}
		});
		
		// Picking a later start time can shrink maxDuration; pull the entered duration back under it.
		effect(() => {
			const max = this.maxDuration();
			const current = this.bookingCreateForm.controls.duration.value;
			
			if (current != null && current > max) {
				this.bookingCreateForm.controls.duration.setValue(clamp(current, MIN_DUR, max));
			}
		});
	}
	
	/** Discards the form and goes back: admins to the bookings admin list, everyone else up one route. */
	onCancel() {
		this.onReset();
		if (this.isAdmin()) {
			this.router.navigate(['admin/bookings'], {
				replaceUrl: true,
			}).then();
		} else {
			this.router.navigate(['..'], {
				relativeTo: this.route,
				replaceUrl: true,
			}).then();
		}
	}
	
	onReset() {
		this.bookingCreateForm.reset();
	}
	
	/** Submits the booking if the payload is complete, then resets the form and navigates up one route. */
	onSave() {
		const payload = this.bookingPayload();
		if (!payload) {
			return;
		}
		
		console.log(this.bookingPayload());
		
		this.bookingStore.create(payload);
		
		this.onReset();
		this.router.navigate(['..'], {
			relativeTo: this.route,
			replaceUrl: true,
		}).then();
	}
	
	// Selected start time in minutes, as a signal.
	startMinutes: Signal<number | null> = toSignal(this.bookingCreateForm.controls.startsAt.valueChanges, {
		initialValue: this.bookingCreateForm.controls.startsAt.value,
	});
}

export default CreateBookingComponent;
