import { formatDate } from '@angular/common';

// All times below are minutes since midnight.
export const DAY_START = 480;    // 08:00  (FR-4.3)
export const DAY_END = 1080;     // 18:00  (FR-4.3)
const STEP = 15;                 // FR-3.1 slot quantum
export const MIN_DUR = 15;       // FR-3.1
const MAX_DUR = 240;             // FR-3.1 (4h)
export const DEFAULT_DUR = 60;   // used for the end time until a duration is entered
export const HORIZON_DAYS = 30;  // FR-3.1: how far ahead a booking can be made

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
export const combineDateAndMinutes = (date: Date, minutes: number): Date => {
	const combined = new Date(date);
	combined.setHours(Math.floor(minutes / 60), minutes % 60, 0, 0);
	return combined;
};

/** Returns value when it lies within the inclusive bounds, otherwise returns whichever bound it exceeded. */
export const clamp = (value: number, min: number, max: number) => {
	return (value < min) ? min : (value > max) ? max : value;
};

/** Longest duration allowed for a given start time: MAX_DUR, or less if that would run past DAY_END. */
export const maxDurationFor = (minutes: number) => {
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
export const startOfToday = () => {
	return new Date(new Date().setHours(0, 0, 0, 0));
};

/** Returns a copy of date moved forward by `days`. */
export const addDays = (date: Date, days: number) => {
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