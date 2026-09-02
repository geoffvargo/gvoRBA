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

const DAY_START = 480;    // 08:00  (FR-4.3)
const DAY_END = 1080;     // 18:00  (FR-4.3)
const STEP = 15;          // FR-3.1 slot quantum
const MIN_DUR = 15;       // FR-3.1
const MAX_DUR = 240;      // FR-3.1 (4h)
const DEFAULT_DUR = 60;
const HORIZON_DAYS = 30;  //FR-3.1

const timerange = (from: number, to: number, step: number) => {
	const ans = [];
	for (let i = from; i < to; i += step) {
		ans.push(i);
	}
	return ans;
};

const pad2 = (num: number) => {
	return String(num).padStart(2, '0');
};

const timeLabel = (minutes: number) => {
	const h = Math.floor(minutes / 60);
	const m = minutes % 60;
	
	const hh: string = pad2(h);
	const mm: string = pad2(m);
	const str: string = hh + ':' + mm;
	console.log(str);
	return str;
};

/* const durationLabel = (minutes: number) => {
 const h = Math.floor(minutes / 60);
 return (h > 0 ? h + 'h ' : '') + (h < 10 ? '0' : '');
 }; */

const START_VALUES = timerange(DAY_START, DAY_END, STEP);
// const DURATION_VALUES = timerange(MIN_DUR, MAX_DUR, STEP);

export const START_OPTIONS: { value: number, label: string }[] = START_VALUES.map(v => ({
	value: v,
	label: timeLabel(v),
}));

/* const DURATION_OPTIONS: { value: number, label: string }[] = DURATION_VALUES.map(v => ({
 value: v,
 label: durationLabel(v),
 })); */

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

const maxDurationFor = (minutes: number) => {
	return Math.min(MAX_DUR, DAY_END - minutes);
};

export const isWeekday = (d: Date | null): boolean => {
	const day = (d ?? new Date()).getDay();
	return day !== 0 && day !== 6;   // 0 = Sunday, 6 = Saturday
};

/* export const nextWeekdayFrom = (from: Date) => {
 const day = new Date(new Date(from).setDate(from.getDate() + 1));
 while (!isWeekday(day)) {
 day.setDate(day.getDate() + 1);
 }
 
 return day;
 }; */

const startOfToday = () => {
	return new Date(new Date().setHours(0, 0, 0, 0));
};

const addDays = (date: Date, days: number) => {
	const copy = new Date(date);
	copy.setDate(copy.getDate() + days);
	return copy;
};

/* export const isSameLocalDay = (a: Date, b: Date) => {
 return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
 }; */

export const formatLocalDate = (date: Date) => {
	return date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate();
};

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
	
	],
	providers: [provideNativeDateAdapter()],
	templateUrl: './create-booking.component.html',
	styleUrl: './create-booking.component.css',
	encapsulation: ViewEncapsulation.None,
})
class CreateBookingComponent {
	private router = inject(Router);
	private route = inject(ActivatedRoute);
	
	private readonly fb = inject(NonNullableFormBuilder);
	
	protected roomStore = inject(RoomStore);
	protected userStore = inject(UserStore);
	protected bookingStore = inject(BookingStore);
	
	protected readonly rooms = this.roomStore.rooms;
	protected readonly users = this.userStore.users;
	protected readonly startOptions = signal(START_OPTIONS);
	protected readonly isWeekdayFilter = isWeekday;
	protected readonly MIN_DUR = MIN_DUR;
	
	private readonly endWithinWorkingHours: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
		const group = control as FormGroup<{
			duration: AbstractControl<number>;
			start: AbstractControl<number>;
		}>;
		
		const { start, duration } = group.getRawValue();
		
		if (start == null || duration == null) {
			return null;
		}
		
		const end = start + duration;
		
		return end > DAY_END ? { endAfterClass: { end, limit: DAY_END, overBy: end - DAY_END } } : null;
	};
	
	bookingCreateForm = this.fb.group({
			roomId: this.fb.control<number>(0, [Validators.required]),
			userId: this.fb.control<number>(0, [Validators.required]),
			date: this.fb.control<Date | null>(null, [Validators.required]),
			startsAt: this.fb.control<number | null>(null, [Validators.required]),
			duration: this.fb.control<number | null>(null, [Validators.required]),
			purpose: this.fb.control('', [Validators.required]),
			bookingStatus: this.fb.control(true, [Validators.required]),
		}, {
			validators: [this.endWithinWorkingHours],
		},
	);
	
	private readonly bookingPayload = computed((): BookingRequest | null => {
		console.log(this.bookingCreateForm.controls.roomId.value);
		
		const { roomId, userId, date, purpose, bookingStatus } = this.formValue();
		const start = this.startMinutes();
		
		if (!roomId || !userId || !date || start == null || !purpose) {
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
			status: bookingStatus && bookingStatus ? 'CONFIRMED' : 'CANCELLED',
		} as BookingRequest;
	});
	
	protected readonly formValue = toSignal(
		this.bookingCreateForm.valueChanges, {
			initialValue: this.bookingCreateForm.value,
		},
	);
	
	today = signal(startOfToday());
	minDate = computed(() => this.today());
	maxDate = computed(() => addDays(this.today(), HORIZON_DAYS));
	maxDuration = computed(() => maxDurationFor(this.startMinutes() ?? DAY_START));
	endMinutes = computed(() => (this.startMinutes() ?? DAY_START) + (this.durationMinutes() ?? DEFAULT_DUR));
	
	durationMinutes: Signal<number | null> = toSignal(this.bookingCreateForm.controls.duration.valueChanges, {
		initialValue: this.bookingCreateForm.controls.duration.value,
	});
	
	constructor() {
		effect(() => {
			const max = this.maxDuration();
			const current = this.bookingCreateForm.controls.duration.value;
			
			if (current != null && current > max) {
				this.bookingCreateForm.controls.duration.setValue(clamp(current, MIN_DUR, max));
			}
		});
	}
	
	onCancel() {
		this.onReset();
		this.router.navigate(['..'], {
			relativeTo: this.route,
			replaceUrl: true,
		}).then();
	}
	
	onReset() {
		this.bookingCreateForm.reset();
	}
	
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
	
	startMinutes: Signal<number | null> = toSignal(this.bookingCreateForm.controls.startsAt.valueChanges, {
		initialValue: this.bookingCreateForm.controls.startsAt.value,
	});
}

export default CreateBookingComponent;
