# Fix: NG0200 circular dependency for `AuthStore`

**Date:** 2026-10-02 22:51:56
**File changed:** `frontend/gvorba-frontend/src/app/interceptors/auth.intercepter-interceptor.ts`

## Symptom

Runtime error in the frontend on page load:

```
RuntimeError: NG0200: Circular dependency detected for `_AuthStore`.
    ...
    at authInterceptor (auth.intercepter-interceptor.ts:13:20)
```

It only appeared when the page loaded or reloaded while the user was logged in,
meaning there was already an `auth-token` in `sessionStorage`.

## Root cause

This loop runs during app startup:

1. Something injects `AuthStore`, and Angular starts building it.
2. The `AuthStore` constructor calls `loadCurrentUser()`.
3. With a token in `sessionStorage`, that calls `apiService.getCurrentUser().subscribe(...)`.
4. Subscribing sends the request through `HttpClient`, which runs `authInterceptor`
   straight away, before step 1 has finished.
5. The interceptor called `inject(AuthStore)` at the top of the function. Angular saw
   that `AuthStore` was still being built and threw **NG0200**.

A fresh visit with no stored token skips the HTTP call in step 3, so the loop never
starts. The eager `inject(AuthStore)` has been in the interceptor since commit `36c23cb`.

## Fix

The interceptor only needs `AuthStore` in one place: the 401 branch, where it calls
`refresh()`. It now looks up `AuthStore` only when a 401 comes back, not on every
request:

```ts
import { inject, Injector } from '@angular/core';
...
const injector = inject(Injector);            // was: inject(AuthStore)
...
return injector.get(AuthStore).refresh()...   // inside catchError, on 401 only
```

### Why `Injector` and not `inject(AuthStore)` inside `catchError`

`inject()` only works while the interceptor function is first running. `catchError`
runs later, when the response arrives, so calling `inject()` there would throw a
different error. Grabbing the `Injector` up front and calling `injector.get()` later
works at any time.

### Why this breaks the loop

A response, including a 401, always arrives after a network round trip. By then the
`AuthStore` constructor has finished, so `injector.get(AuthStore)` returns the
existing instance. It's still the same single `AuthStore`, and the refresh-and-retry
behaviour doesn't change.

## Alternative considered

Moving `loadCurrentUser()` out of the `AuthStore` constructor, for example into an app
initializer or the root component, would also break the loop. It was rejected because
any future code that calls the API during the constructor would bring the loop back.
The interceptor-side fix removes the dependency at its source.

## Verification

- `tsc -p tsconfig.app.json --noEmit` passes.
- To check in the browser: log in, reload the page, and confirm NG0200 no longer
  appears and the current user still loads.
- No interceptor spec exists, so no tests cover this change.

## Related note

`app.config.ts` calls `provideHttpClient()` twice, once plain and once with
`withInterceptors([authInterceptor])`. It doesn't cause this bug, but the plain call
can be removed.