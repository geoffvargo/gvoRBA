# gvoRBA Platform - Specification

- **Version:** 0.2 (Draft)
- **Status:** Living document — updated as decisions are made
- **Last updated:** 2026-10-07
- **Reflects:** `master` @ `ce7c3b8` (merge of PR #79)

---

## 1. Overview

This is a web application that lets employees within an organization browse for and book
conference rooms for meetings. The system enforces availability rules, prevents double-bookings
of rooms, and gives administrators tools for managing rooms, users, and bookings.

This project is built as a portfolio-piece demonstrating full-stack development with **Angular**
(frontend) and **Spring Boot** (backend), backed by **PostgreSQL**, with stateless JWT authentication,
OAuth2 social sign-in, and a layered backend architecture.

## 2. Goals and Non-goals

### Goals

- Authenticated users can find and book available conference rooms.
- The system guarantees the core invariant: **no two non-cancelled bookings of a room may
  overlap**.
- Administrators can manage the room catalog, view all bookings, and make adjustments to
  user accounts and roles.
- Members can sign in with a local account or with Google / GitHub.
- Confirmed bookings can leave the app as calendar entries (per-booking `.ics`, one-way Google
  Calendar sync).
- The application is deployed live and demonstrates clean architectural
  decisions documented in ADRs.

### Non-goals (v1)

The following are deliberately excluded from the first version to keep the scope manageable:

- Payments, billing, or any pricing model.
- Multi-organization or multi-tenant support.
- Recurring bookings (e.g., "every Tuesday at 10am").
- Booking of resources other than rooms (e.g., desks, equipment, parking, etc.).
- Two-way calendar sync, and Outlook / Microsoft Graph integration. (One-way export is in
  scope; see FR-6.x.)
- Real-time push notifications or mobile apps.
- Waitlisting for unavailable slots.

Several of these are revisited in §14 as candidates for future enhancements.

## 3. Users and Roles

| Role   | Backend authority | Description                                                                                                 |
|--------|-------------------|-------------------------------------------------------------------------------------------------------------|
| Guest  | —                 | Unauthenticated visitor. Can access only the landing (sign-in) and sign-up pages.                           |
| Member | `ROLE_USER`       | Authenticated employee, who can browse rooms, create & manage their own bookings.                           |
| Admin  | `ROLE_ADMIN`      | Has all permissions of a Member, and can also manage rooms, view all bookings, and manage user accounts.    |

Further points:

- A user has exactly one role at a time (`User.role` → `Role`).
- The default role for all new registrations — local sign-up and first OAuth2 sign-in alike — is
  `ROLE_USER`.
- Role changes are performed by Admins only.

## 4. Functional Requirements

Numbered for traceability. Each requirement is testable.

### 4.1 Authentication and Account Management

- **FR-1.1** A guest can register with username, email, and password. Passwords are
  hashed with BCrypt (strength ≥ 10).
- **FR-1.2** A registered user can sign in with username and password and
  receive an access token (response body) and refresh token (HttpOnly cookie). See ADR-0002.
- **FR-1.3** A signed-in user can refresh their access token using the
  refresh token cookie without re-entering credentials. Each refresh rotates the refresh token.
- **FR-1.4** A signed-in user can sign out, which revokes the current
  refresh token and clears the cookie.
- **FR-1.5** A signed-in user can view their own profile (email, role,
  registration date).
- **FR-1.6** Passwords must be at least 8 characters with at least one
  letter and one number. Validation is enforced on both client and server.
- **FR-1.7** A guest can sign in with **Google** (OIDC) or **GitHub** (OAuth2). Account resolution, in order:
    1. An account already linked to (`authProvider`, `providerId`) is used as-is.
    2. Otherwise, an existing account with the same email (case-insensitive) is linked to the
       provider and used.
    3. Otherwise, a new `ROLE_USER` account is provisioned with no local password.

  A blank or unverified provider email is rejected. On success the backend sets the refresh-token
  cookie and redirects to the frontend `/oauth2/callback` route, which exchanges the cookie for an
  access token via the refresh endpoint. On failure the backend redirects to the same route with
  `?error=$ErrorCode`.
- **FR-1.8** Behind CloudFront, the OAuth2 `redirect_uri` is pinned to `OAUTH2_BASE_URL` (the public
  CloudFront origin). When unset (local runs), Spring's `{baseUrl}` is used.
  See `docs/fixes/oauth2-aws-fix-2026-09-25.md`.

### 4.2 Room catalog

- **FR-2.1** A member can view a list of all rooms, with name, capacity, location, and amenities.
- **FR-2.2** A member can filter rooms by minimum capacity and by name.
- **FR-2.3** A member can view a single room's details, including a week-view calendar of its existing bookings.
  The calendar renders whole business weeks (Mon–Fri, per FR-4.3). The displayed span is selectable
  between a minimum of one business week and a maximum of four business weeks ("one business month"),
  and supports navigation forward and back across the 30-day booking horizon defined in FR-3.1. The
  calendar never displays a span shorter than one business week, on any viewport.
- **FR-2.4** An admin can create a new room with name, capacity, location, and amenities. Room name and
  location must each be unique.
- **FR-2.5** An admin can edit a room's metadata.
- **FR-2.6** An admin can deactivate a room. Deactivated rooms cannot be booked, but existing future bookings still
  remain visible. Deactivation is soft; rooms are not hard-deleted.

### 4.3 Booking Lifecycle

- **FR-3.1** A member can create a booking for an active room by selecting a date, start time, and duration
  (end time is derived). Bookings must:
    - Start at a 15-minute boundary (`:00`, `:15`, `:30`, `:45`).
    - Be at least 15 minutes long and at most 4 hours long.
    - Start in the future (not in the past).
    - Start no more than 30 days in the future.
- **FR-3.2** The system must reject a booking that overlaps any existing non-cancelled booking on the same room,
  with HTTP 409 Conflict (`BOOKING_CONFLICT`). See ADR-0001.
- **FR-3.3** A member can view their own upcoming and past bookings, sorted by start time descending.
- **FR-3.4** A member can cancel one of their own future bookings. Past bookings cannot be canceled.
- **FR-3.5** An admin can view all bookings across all rooms and members.
- **FR-3.6** An admin can cancel any future booking (e.g., to free a room for maintenance). Cancellation by an admin
  records the actor for audit.
- **FR-3.7** Canceled bookings are soft-deleted (retained with `status = CANCELLED`, a `cancelled_at` timestamp,
  and a `cancelled_by` reference) for audit history.
- **FR-3.8** The booking owner (or an admin) can restore a cancelled booking ("uncancel"). The restored booking is
  subject to FR-3.2: if the slot has since been taken, the restore is rejected with 409.
- **FR-3.9** The booking owner (or an admin) can edit a booking's date, start time, duration, purpose, and
  attendees. Edits are validated against FR-3.1 and FR-3.2. The room and owner cannot be changed by an
  edit.
- **FR-3.10** A booking may list zero or more other users as **attendees**. Attendees are selected from the user
  directory when creating or editing a booking and are shown on the booking details page.

### 4.4 Availability

- **FR-4.1** Given a room and an inclusive date range, the system returns the room's bookings within that range.
  The range may not exceed 31 days; a request for a longer range is rejected with `VALIDATION_FAILED`.
  When only a start date is supplied, the range collapses to that single day — used by the
  409-conflict refresh, not by any calendar view. Four business weeks, the largest span FR-2.3
  permits, occupies at most 26 calendar days and therefore always fits. See ADR-0004.
- **FR-4.2** When provided with a valid range of dates (within working hours) and a minimum capacity, the system will
  return a list of available rooms, each with at least one time slot satisfying the specified parameters.
- **FR-4.3** Default working hours are Mon-Fri, 08:00-18:00, but can be customized by the end-user organization.
- **FR-4.4** Booking requests outside working hours are rejected outright.

### 4.5 Administration

- **FR-5.1** An admin can view a list of all users and their roles.
- **FR-5.2** Admins can promote/demote members to/from the admin role. However, admins cannot change their own role.
- **FR-5.3** User accounts can be deactivated (and reactivated) by admins. Admins cannot deactivate themselves.
  Once deactivated, users can no longer log in, and all their pending bookings are automatically canceled.
- **FR-5.4** An admin can create a local user account (name, email, password, role) and edit an existing user's
  name, email, role, and enabled flag.

### 4.6 Calendar Export

- **FR-6.1** The owner of a **confirmed** booking (or an admin) can download it as an iCalendar file
  (`text/calendar`, RFC 5545). The event carries a stable UID derived from the booking ID, the booking
  purpose as summary, and `"$RoomName, $RoomLocation"` as `LOCATION`. Cancelled bookings cannot be exported.
- **FR-6.2** When Google Calendar integration is enabled (`app.google-calendar.enabled=true`), every booking
  create, edit, cancel, uncancel, and delete is mirrored one-way to a shared Google Calendar:
    - Sync runs asynchronously after the booking transaction commits; a rolled-back booking never
      reaches Google, and a Google failure never fails the booking request (it is logged).
    - Each booking maps to exactly one Google event with the deterministic ID `booking$BookingId`, so
      re-syncs are idempotent upserts.
    - Cancelled/deleted bookings delete their Google event; a missing event (404/410) counts as success.
- **FR-6.3** When Google Calendar integration is enabled, a user can download all events in the shared calendar
  for an inclusive date range as a single `.ics` file. The range may not exceed 366 days and `to` may
  not precede `from` (400); a Google API failure returns 502.

## 5. Non-Functional Requirements

- **NFR-1 (Security).** All API endpoints except `/api/health`, `/api/ping`, `/api/auth/public/**`,
  and Spring's OAuth2 endpoints (`/oauth2/**`, `/login/oauth2/**`) require a valid access token.
  Admin-only endpoints are additionally restricted to `ROLE_ADMIN` on the server, not only by
  frontend route guards. Passwords and raw refresh tokens are never logged or placed in URLs.
- **NFR-2 (Concurrency).** The system must correctly reject double-bookings under concurrent load
  (verified by an integration test that fires simultaneous booking requests for the same slot).
- **NFR-3 (Performance).** The room list endpoint must return within 200 ms p95 for a catalog of up to
  200 rooms.
- **NFR-4 (Observability).** All requests are logged (`CustomLoggingFilter`).
- **NFR-5 (Portability).** The backend and database run locally via `docker compose up`.
- **NFR-6 (Testability).** Frontend stores and the OAuth2 callback have Vitest unit tests. The backend
  must have integration tests covering the booking-conflict path.
- **NFR-7 (Bundle size).** The production frontend bundle error budget is 1.1 MB.

## 6. Domain Model

All primary keys are `BIGINT` sequences. Timestamps are stored zone-less (`LocalDateTime`) and
interpreted in the configured calendar time zone where a zone is required (Google sync).

### Entities

| **User**                                                       |
|----------------------------------------------------------------|
| `id` (Long, PK)                                                |
| `name` (string — also the login username)                      |
| `email` (string; matched case-insensitively for OAuth2 linking)|
| `password` (BCrypt hash; `null` for OAuth2-provisioned users)  |
| `role` (FK → Role: `ROLE_USER` \| `ROLE_ADMIN`)                |
| `createdOn` (Date)                                             |
| `enabled` (boolean, not null)                                  |
| `authProvider` (`LOCAL` \| `GOOGLE` \| `GITHUB`, default `LOCAL`) |
| `providerId` (string, nullable)                                |
| **Constraint** `UNIQUE (auth_provider, provider_id)` (V6)      |

| **Room**                                  |
|-------------------------------------------|
| `id` (Long, PK)                           |
| `name` (not null, unique by app check)    |
| `location` (e.g., "Building A, Floor 2")  |
| `capacity` (int)                          |
| `amenities` (list of strings)             |
| `isActive` (boolean - soft-delete flag)   |
| `createdOn` (timestamp)                   |

| **Booking**                                                                                                                                       |
|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `id` (Long, PK)                                                                                                                                   |
| `room` (FK → Room, not null)                                                                                                                      |
| `userId` (FK → User, not null — the owner)                                                                                                        |
| `startsAt`, `endsAt` (timestamp, not null)                                                                                                        |
| `purpose` (short text, optional)                                                                                                                  |
| `status` (`CONFIRMED` \| `CANCELLED`)                                                                                                             |
| `cancelledAt` (timestamp, nullable); `cancelled_by` (FK → User, nullable — column exists, not yet mapped)                                         |
| `attendees` (M:N → User via `booking_attendee`)                                                                                                   |
| **Constraint** `bookings_no_overlap EXCLUDE USING gist (room_id WITH =, tsrange(starts_at, ends_at, '[)') WITH &&) WHERE (cancelled_at IS NULL)` (V3) |

| **BookingAttendee** (join table `booking_attendee`, V8)                        |
|--------------------------------------------------------------------------------|
| `booking_id` (FK → bookings, `ON DELETE CASCADE`) + `user_id` (FK → users) — composite PK |
| Index on `user_id`                                                             |

| **RefreshToken**                   |
|------------------------------------|
| `id` (Long, PK)                    |
| `user` (FK → User)                 |
| `tokenHash` (unique, not null)     |
| `expiresAt` (timestamp, not null)  |
| `revokedAt` (timestamp, nullable)  |

| **AuthHandoffCode** (V7 — reserved, not wired)   |
|--------------------------------------------------|
| `id` (Long, PK)                                  |
| `user` (FK → User)                               |
| `codeHash`, `expiresAt`, `consumedAt`            |

`AuthHandoffCode` has a table and entity but no code creates or reads it; the OAuth2 flow hands off
via the refresh-token cookie instead (FR-1.7).

### Migrations (Flyway, `backend/gvoRBA-Backend/src/main/resources/db/migration/`)

| Version | File                                      | Change                                                 |
|---------|-------------------------------------------|--------------------------------------------------------|
| V1      | `V1__enable_extensions.sql`               | PostgreSQL extensions                                  |
| V2      | `V2__create_tables.sql`                   | Base tables                                            |
| V3      | `V3__exclusion_constraint.sql`            | `cancelled_at`, `cancelled_by`, `bookings_no_overlap`  |
| V4      | `V4__booking_status_to_varchar.sql`       | `status` → varchar                                     |
| V5      | `V5__user_is_enabled.sql`                 | `users.enabled`                                        |
| V6      | `V6__oauth_provider_columns.sql`          | `auth_provider`, `provider_id`, unique pair            |
| V7      | `V7__create_auth_handoff_code_table.sql`  | `auth_handoff_code` table                              |
| V8      | `V8__booking_attendee.sql`                | `booking_attendee` join table                          |

A full ERD is maintained in `docs/erd.png`, and will be regenerated when/if the schema changes.

## 7. API surface

REST over HTTPS, JSON request/response. This section is the inventory of what `master` exposes.

### Auth (`/api/auth`)

- `POST /public/signup` – create a local account (`ROLE_USER`)
- `POST /public/signin` – exchange username + password for access token (body) + refresh token (cookie)
- `POST /public/refresh` – rotate the refresh-token cookie and return a new access token
- `POST /public/logout` – revoke the current refresh token and clear the cookie (204)
- `GET /me` – current user profile
- `GET /getUser` – current user profile (duplicate of `/me`)

Refresh cookie: `refreshToken`, `HttpOnly`, `Secure`, `Path=/api/auth`, `SameSite` from
`app.cookie.same-site`, max-age from `app.refresh.ttl-days`.

### OAuth2 (Spring Security defaults)

- `GET /oauth2/authorization/{google|github}` – start the provider login
- `GET /login/oauth2/code/{google|github}` – provider callback; ends in a redirect to
  `app.oauth2.frontend-redirect-uri` (FR-1.7)

### Rooms (`/api/rooms`)

- `GET /` – list rooms; optional `?name=` (substring) and `?minCapacity=`
- `GET /{id}` – room details
- `GET /{id}/bookings` – bookings for a room; optional `?date=` (ISO date-time)
- `POST /add-room` – create room (admin)
- `POST /add-rooms` – bulk create; duplicates by name or location are skipped (admin)
- `PUT /{id}` – update room (admin)
- `DELETE /{id}` – deactivate room (soft) (admin)

### Bookings (`/api/bookings`)

- `GET /` – list all bookings (admin)
- `GET /{id}` – booking details
- `GET /me` – current user's bookings
- `POST /add-booking` – create booking (body: `roomId`, `userId`, `startsAt`, `endsAt`, `purpose`,
  `status`, `attendees?: Long[]`); 409 on conflict
- `POST /add-bookings` – bulk create; conflicting entries are skipped
- `PATCH /{id}/edit` – edit booking (same body as create; `attendees` replaces the set); 409 on conflict
- `PATCH /cancel/{id}` – soft-cancel (owner or admin)
- `PATCH /uncancel/{id}` – restore a cancelled booking (owner or admin)
- `DELETE /delete/{id}` – hard-delete (owner or admin)
- `GET /{id}/calendar.ics` – download a confirmed booking as `.ics` (FR-6.1)

### Calendar (`/api/calendar`) — registered only when `app.google-calendar.enabled=true`

- `GET /export.ics?from=YYYY-MM-DD&to=YYYY-MM-DD` – Google Calendar events in range as `.ics` (FR-6.3)
- `GET /booking.ics?bookingId=` – stub; currently returns an empty body

### Users (`/api/users`)

- `GET /` – list all users (admin)
- `GET /{id}` – user details (admin)
- `PUT /create` – create a local user (admin)
- `PATCH /{id}/update` – update name, email, role, enabled (admin)
- `PATCH /{id}/role` – change role (admin; cannot change own role)
- `PATCH /{id}/toggle-active` – activate/deactivate user (admin; cannot target self)

### Misc

- `GET /api/health` – liveness (public)
- `GET /api/ping` – `{ status, timestamp }` (public)
- `GET /api/dev/oauth2-echo` – echoes query params; `dev` profile only

### Error Model

Target shape for all error responses:

```
{
  "timestamp": "YYYY-MM-DDTHH:MM:SSZ",
  "status": 409,
  "code": [string],
  "message": [string],
  "path": [string (path to endpoint)]
}
```

Documented error codes: `VALIDATION_FAILED`, `AUTHENTICATION_REQUIRED`,
`FORBIDDEN`, `NOT_FOUND`, `BOOKING_CONFLICT`, `OUT_OF_HOURS`,
`ROOM_INACTIVE`, `INTERNAL_ERROR`.

## 8. Frontend (Angular)

### Routes

Defined in `frontend/gvorba-frontend/src/app/app.routes.ts`.

| Path                  | Guard  | Purpose                                     |
|-----------------------|--------|---------------------------------------------|
| `/`                   | none   | Landing page (sign-in)                      |
| `/signup`             | none   | Local registration                          |
| `/access-denied`      | none   | Shown when a guard rejects navigation       |
| `/oauth2/callback`    | none   | OAuth2 handoff (FR-1.7)                     |
| `/home`               | member | User home                                   |
| `/rooms`              | member | Browse and filter rooms                     |
| `/rooms/:id`          | member | Room detail & week calendar                 |
| `/bookings`           | member | Current user's bookings                     |
| `/bookings/create`    | member | Booking form                                |
| `/bookings/:id`       | member | Booking details: edit, cancel/uncancel, `.ics` export |
| `/admin/rooms`        | admin  | Manage room inventory                       |
| `/admin/rooms/create` | admin  | Create room                                 |
| `/admin/rooms/:id`    | admin  | Edit / deactivate room                      |
| `/admin/bookings`     | admin  | All bookings (filterable)                   |
| `/admin/users`        | admin  | Manage users                                |
| `/admin/users/create` | admin  | Create user                                 |
| `/admin/users/:id`    | admin  | Edit user, role, active flag                |

### State

As per ADR-0003, state is a signal residing in an injectable service. The stores are: `AuthStore`, `RoomStore`,
`BookingStore`, `UserStore` (admin only). HTTP calls return observables, which are stored as signals in the stores.
`BookingStore.currentBooking` holds the booking shown on `/bookings/:id`; `BookingStore.updateBooking()` reloads
it only after the PATCH succeeds (see `docs/fixes/2026-10-06_024717_booking-details-stale-after-save.md`).

### Notable UX Behaviors

- Store operations report success and failure with toast notifications (`@ngxpert/hot-toast`, bottom-center,
  dismissible), configured once via `provideHotToastConfig()` in `app.config.ts`.
- On booking conflict (409), the form shows an inline message and refreshes
  the bookings for the affected day so the user sees the now-taken slot. The refresh issues a
  single-day request rather than re-fetching the whole week.
- A loading spinner is shown while a store request is in flight.
- The booking form uses a `MatDatepicker` (weekdays only, today through `HORIZON_DAYS` ahead) and two
  `mat-select` dropdowns (start time, duration); end time is derived and read-only. Attendees are a
  multi-select of users. The same controls are reused for editing on `/bookings/:id`, pre-populated
  from the booking; the edit layout is responsive (PR #79).
- The calendar on `/rooms/:id` is a hand-rolled CSS Grid + subgrid — no third-party calendar
  library (ADR-0005). It renders between one and four business weeks (Mon–Fri) as a column per day, with a
  control to change the span. It never collapses below one business week; on narrow viewports the
  grid scrolls horizontally, with the time gutter pinned.
- The calendar is read-only. Booking creation happens on the booking form route; the calendar
  may deep-link into that form with a pre-filled start time, but performs no mutations itself.
- Sortable table headers are centered globally (sort arrow offset), with no per-table overrides.
- `authInterceptor` resolves `AuthStore` via `Injector.get()` only on a 401, which avoids the NG0200
  circular-DI error at startup (see `docs/fixes/2026-10-02_225156_ng0200-authstore-circular-dependency.md`).

## 9. Design Decisions

Captured as ADRs in `docs/adr/`. Current set:

- **ADR-0001** — Optimistic locking + DB exclusion constraint for booking
  conflict prevention.
- **ADR-0002** — Stateless JWT authentication with split token storage.
- **ADR-0003** — Angular signals for client-side state management.
- **ADR-0004** — Range-scoped room bookings endpoint (supersedes the original
  single-date endpoint).
- **ADR-0005** — Room calendar rendered with CSS Grid + subgrid driven by Angular style bindings.

Decisions made in code but not yet written up as ADRs: OAuth2 handoff via refresh-token cookie
(rather than a one-time code), and one-way Google Calendar sync via after-commit async events.

Fix write-ups live in `docs/fixes/`.

## 10. Phasing

The project is built in vertical slices. Each milestone is independently
demo-able and deployable.

### Milestone 1 — Skeleton

- Repo scaffolded, both apps run locally and on live URLs.
- `/api/health` and `/api/ping` round-trip working end-to-end.
- CI runs tests on every push.

### Milestone 2 — Catalog

- User registration and login (JWT).
- Read-only room list and detail pages.
- Spring Security wired with role-based authorization.

### Milestone 3 — Bookings

- Booking create/cancel/list.
- Booking edit, uncancel, and attendees.
- Concurrency control verified by integration test.
- Week-view calendar on the room detail page.

### Milestone 4 — Admin

- Admin room and user management.
- Admin booking oversight.

### Milestone 5 — Polish

- OpenAPI/Swagger published.
- Test coverage targets met.
- Dockerfile and docker-compose for full local startup.
- README, ADRs, ERD finalized.

### Milestone 6 — Integrations

- OAuth2 sign-in with Google and GitHub, including account linking. **Done** (PRs #66–#68).
- Per-booking `.ics` export and one-way Google Calendar sync. **Done** (PRs #72–#73).
- Toast notifications. **Done** (PR #74).

## 11. Glossary

- **Booking** — A reservation of a specific room for a specific time range by a specific member.
- **Attendee** — A user listed on a booking other than its owner.
- **Conflict** — An attempt to create or modify a booking such that it overlaps an existing non-canceled booking on the
  same room.
- **Slot** — A 15-minute boundary at which a booking may start or end.
- **Working hours** — The configured time window during which bookings are permitted.
- **Optimistic locking** — Concurrency control strategy that detects conflicting updates at commit time using a version
  field, rather than holding a lock during the transaction. See ADR-0001.
- **Provider** — An external identity provider (Google, GitHub) used for OAuth2 sign-in.

## 12. Known Gaps (spec vs. `master`)

Each item is a requirement above that `master` does not yet meet. File and member names are where the gap lives.

| Requirement      | Gap on `master`                                                                                                                                                          | Location                                                        |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------|
| NFR-1            | No server-side role checks: every non-public endpoint only requires authentication; admin restriction is enforced by Angular `adminGuard` alone.                         | `SecurityConfig.securityFilterChain`; all controllers           |
| FR-6.1           | `downloadIcs()` always passes `isAdmin = true`, so any authenticated user can export any confirmed booking.                                                              | `BookingController.downloadIcs` → `BookingIcsService.exportIcs` |
| FR-3.9           | `editBooking()` performs no ownership check and does not publish a `BookingCalendarEvent`, so edits never reach Google Calendar.                                         | `BookingController.editBooking`                                 |
| FR-3.8           | `uncancelBooking()` does not catch `DataIntegrityViolationException`, so a taken slot is not translated to `OverlapConflictException` (`BOOKING_CONFLICT`).               | `BookingController.uncancelBooking`                             |
| FR-3.6 / FR-3.7  | `cancelled_by` is never written; the column is unmapped on `Booking`.                                                                                                    | `Booking`, `BookingController.cancelBooking`                    |
| FR-3.1 / FR-4.4  | No server-side validation of slot boundary, duration, horizon, or working hours; enforced only by the booking form.                                                      | `BookingController.addBooking`, `editBooking`                   |
| FR-3.2 (bulk)    | `addBookings()` does not persist `attendees`.                                                                                                                            | `BookingController.addBookings`                                 |
| FR-4.1           | Range endpoint not implemented; backend accepts a single optional `?date=` date-time.                                                                                    | `RoomController.getBookings`                                    |
| FR-5.3           | Deactivating a user does not cancel their future bookings.                                                                                                               | `AdminController.toggleActive`                                  |
| FR-2.4           | Duplicate name/location throws `UsernameNotFoundException` instead of a 409/validation error.                                                                            | `RoomController.addRoom`                                        |
| ADR-0001         | `Booking` has no `@Version` field; conflict prevention relies on the exclusion constraint alone.                                                                         | `Booking`                                                       |
| Error model (§7) | Several paths return ad-hoc bodies (e.g. sign-in failure returns 404 with `{message, error}`; sign-up duplicate returns 400 plain text).                                  | `AuthController.signin`, `signup`                               |
| FR-6.3           | `GET /api/calendar/booking.ics` is a stub returning `null`.                                                                                                              | `CalendarExportController.exportBookingToIcs`                   |
| —                | `provideHttpClient()` is registered twice (plain, then with `withInterceptors`).                                                                                         | `frontend/gvorba-frontend/src/app/app.config.ts`                |

## 13. Future Goals and Improvements

§14 lists new product capabilities. This section covers the existing product: closing the §12 gaps,
hardening, and engineering quality. Items are ordered by priority within each subsection.

### 13.1 Goals

- **G-1 Spec parity.** Every §12 row is either closed in code or resolved by amending the requirement.
- **G-2 Server is authoritative.** Every rule in FR-2.6, FR-3.1, FR-4.4, and every role restriction is
  enforced by the backend. Frontend validation exists for UX only.
- **G-3 One error contract.** Every non-2xx response uses the §7 error model.
- **G-4 Decisions are written down.** Every architectural decision made in code has an ADR.
- **G-5 The core invariant is proven in CI.** NFR-2 runs on every push against real PostgreSQL.

### 13.2 Security and authorization

- **IMP-1** Restrict admin endpoints to `ROLE_ADMIN` on the server (method-level or path-level rules).
  `adminGuard` remains as UX. Closes NFR-1.
- **IMP-2** Derive the booking owner from the authenticated principal. `BookingController.addBooking`
  currently trusts `BookingRequest.userId` from the request body, so a member can create bookings
  owned by another user.
- **IMP-3** Pass the caller's real admin status from `BookingController.downloadIcs` to
  `BookingIcsService.exportIcs`. Closes the FR-6.1 gap.
- **IMP-4** Add an owner-or-admin check to `BookingController.editBooking`. Closes the FR-3.9 gap.
- **IMP-5** Restrict the `user1` / `admin` seed accounts (password `password1`) created by
  `SecurityConfig.initData` to the `dev` profile. `initData` has no `@Profile`, so it runs in every
  profile.
- **IMP-6** Remove the per-controller `@CrossOrigin("http://localhost:4200")` annotations from
  `BookingController`, `RoomController`, and `AdminController`. `SecurityConfig.corsConfigurationSource`
  is the single CORS source.

### 13.3 Booking correctness

- **IMP-7** Introduce a `BookingService` that owns all booking rules (FR-2.6, FR-3.1, FR-3.2, FR-4.4).
  `BookingController` currently calls repositories directly, so validation has no single home.
- **IMP-8** Translate `DataIntegrityViolationException` to `BOOKING_CONFLICT` on every write path
  (create, bulk create, edit, uncancel).
- **IMP-9** Map `cancelled_by` on `Booking` and set it on cancel. Closes FR-3.6 / FR-3.7.
- **IMP-10** Publish a `BookingCalendarEvent` from `editBooking` so edits reach Google Calendar (FR-6.2).
- **IMP-11** Cancel a user's future bookings when `AdminController.toggleActive` deactivates them (FR-5.3).
- **IMP-12** Implement the `from` / `to` range contract on `RoomController.getBookings` per ADR-0004 (FR-4.1).
- **IMP-13** Make time zones consistent. Bookings are stored as zone-less `LocalDateTime`.
  `GoogleCalendarSyncService` interprets them in the configured calendar zone, but
  `BookingIcsService.exportIcs` interprets them as UTC (`toInstant(ZoneOffset.UTC)`). The same booking
  therefore exports at different times through the two paths. Pick one zone source and use it in both.
- **IMP-14** Either add `@Version` to `Booking` as ADR-0001 specifies, or amend ADR-0001 to rely on the
  exclusion constraint alone.

### 13.4 API and performance

- **IMP-15** Add a global exception handler that emits the §7 error model. Replace the misuse of
  `UsernameNotFoundException` for missing rooms and duplicate rooms in `RoomController`.
- **IMP-16** Replace in-memory filtering with repository queries. `BookingController.getMyBookings`
  calls `findAll()` and filters by owner. `RoomController.getAllRooms` calls `findAll()` and filters by
  name and capacity. Both scale with table size (NFR-3).
- **IMP-17** Move to resource-oriented paths (`POST /api/bookings`, `DELETE /api/bookings/{id}`,
  `POST /api/bookings/{id}/cancel`, `POST /api/rooms`) and drop the duplicate `GET /api/auth/getUser`.
  The frontend `ApiService` must change in the same PR.
- **IMP-18** Implement or remove the `GET /api/calendar/booking.ics` stub.
- **IMP-19** Publish OpenAPI / Swagger UI (Milestone 5).

### 13.5 Testing and CI

- **IMP-20** Add the NFR-2 concurrency test against real PostgreSQL (e.g. Testcontainers). The
  `bookings_no_overlap` gist exclusion constraint is PostgreSQL-specific, so an in-memory database
  cannot exercise it.
- **IMP-21** Add a spec for `authInterceptor` (none exists, per the NG0200 fix write-up) and for
  `BookingStore.updateBooking`'s reload-after-PATCH behaviour (none exists, per the stale-details fix
  write-up).
- **IMP-22** Add backend tests for the three account-resolution branches of
  `OAuth2UserProvisioningService.provision` (FR-1.7).

### 13.6 Frontend

- **IMP-23** Remove the duplicate `provideHttpClient()` in `app.config.ts`.
- **IMP-24** Show the server's error-model `message` in toasts. The stores currently toast
  `HttpErrorResponse.message`, which is Angular's transport-level text, not the API's message.
  Depends on IMP-15.
- **IMP-25** Remove debug `console.log` calls from stores and components.

### 13.7 Documentation

- **IMP-26** Write ADRs for: the OAuth2 handoff via refresh-token cookie, the one-way Google Calendar
  sync design, and the attendees model.
- **IMP-27** Decide the fate of `AuthHandoffCode`: wire it into the OAuth2 flow, or drop it with a new
  migration that removes `auth_handoff_code` (V7). Record the choice in the OAuth2 ADR (IMP-26).
- **IMP-28** Regenerate `docs/erd.png` to include V6–V8.
- **IMP-29** Reconcile ADR-0005's "Known defects" list (`dayMapper` off-by-one, `rowGridMapper` offset,
  template-called mappers) with the current calendar code, and mark each fixed or open.

## 14. Future Enhancements

The non-goals listed in §2, plus the features below that extend the current product, are candidates for versions
after v1. None are committed; each would be specified with its own functional requirements (and an ADR where it changes the architecture) before work begins.

- **External calendar integrations — remaining scope.** One-way Google sync and per-booking ICS export are implemented (§4.6). Remaining: two-way sync,
  Outlook/Microsoft Graph, and per-user (rather than shared) calendars.
- **Amenity-based room search.** Extend FR-2.2 so members can filter rooms by required amenities (e.g. "Projector"
  and "Video Conferencing") alongside minimum capacity and name. `Room.amenities` already stores free-text strings, so
  this first needs a controlled amenity vocabulary that admins manage, to stop variants like "WiFi" and "Wi-Fi" from
  splitting results.
- **Waitlisting.** Members can join a waitlist for a taken slot and are offered it when the booking is cancelled. Depends on
  notifications (below) to be useful.
- **Notifications.** Email, and later real-time push, for booking confirmations, cancellations (especially admin-initiated ones,
  FR-3.6), attendee invitations (FR-3.10), and waitlist offers.
- **Check-in and no-show release.** A booking must be checked in within a grace period after `startsAt`; unclaimed
  bookings are auto-cancelled so the room returns to availability. Open questions: the grace period length, whether
  check-in happens in-app or from a room-side device, and how auto-cancellation is recorded under FR-3.7.
- **Non-room resources.** Generalizing `Room` into a bookable `Resource` (desks, equipment, parking) with per-type booking rules.
- **Multi-tenancy.** Supporting multiple organizations, each with its own rooms, users, and working hours (FR-4.3). Requires an
  organization scope on every entity and on authorization checks.
- **Utilization reporting.** An admin view of booked hours per room, peak times, cancellation and no-show rates, and
  capacity fit (attendee count vs. `capacity`), over a selectable date range. Built from existing `bookings` and
  `booking_attendee` data; informs room inventory decisions under FR-2.4–FR-2.6.
