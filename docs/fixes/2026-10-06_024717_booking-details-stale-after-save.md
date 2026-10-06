# Fix: Booking details page shows old data after saving an edit

**Date:** 2026-10-06 02:47:17
**Files changed:**
- `frontend/gvorba-frontend/src/app/stores/booking-store.ts`
- `frontend/gvorba-frontend/src/app/protected/booking-details.component.ts`

## Symptom

On the booking details page, editing a booking and clicking **Save** saved the
changes on the server and showed the "Booking updated successfully." toast. The
details on the page still showed the old values until a full page refresh.

## Root cause

A race between two HTTP requests. `onSave()` did this:

```ts
this.bookingStore.updateBooking(this.bookingId(), payload);   // PATCH
...
this.bookingStore.loadBooking(this.bookingId());              // GET
```

`updateBooking()` only starts the PATCH and returns right away. It doesn't wait
for the response. So `loadBooking()` sent its GET immediately, which usually
reached the server before the PATCH had been applied, and it loaded the old
booking into `currentBooking`.

When the PATCH finished, `updateBooking`'s success handler only called
`loadBookings()`, which refreshes the bookings list. It never refreshed
`currentBooking`, which is what the details page displays.

## Fix

`booking-store.ts`: `updateBooking`'s success handler now reloads the current
booking, after the server has confirmed the update:

```ts
next: data => {
	console.log(data);
	this._isLoading.set(false);
	// Reload only after the server has applied the update, so the details page shows the new values.
	this.loadBooking(id);
	this.loadBookings();
	...
},
```

`_isLoading.set(false)` was moved before the reload calls. Before, it ran last,
so it switched the spinner off while `loadBooking()` was still running.

`booking-details.component.ts`: the early `loadBooking()` call was removed from
`onSave()`:

```ts
onSave() {
	const payload = this.bookingPayload();
	
	if (!payload) {
		return;
	}
	
	this.bookingStore.updateBooking(this.bookingId(), payload);
	
	this.editingForm.reset();
	this.isEditing.set(false);
}
```

When the GET returns, `currentBooking` updates. That re-renders the details
view, and the `populateForm()` effect refills the edit form with the saved
values.

## Alternative considered

If the PATCH endpoint returns the updated booking, the success handler could
call `this.currentBooking.set(data)` and skip the extra GET. That wasn't done
because it relies on the shape of the endpoint's response. Reloading works
whatever the PATCH returns.

## Verification

- Not yet tested in the browser. To check: open a booking, click **Edit**,
  change a field, click **Save**, and confirm the details update without a
  page refresh.
- No spec covers `updateBooking`'s reload behaviour.