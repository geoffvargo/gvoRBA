# gvoRBA Platform - Specification

**Version:** 0.1 (Draft)
**Status:** Living document — updated as decisions are made
**Last updated:** 2026-06-08

---

## 1. Overview

This is a web application that lets employees within an organization browse for and book
conference rooms for meetings. The system enforces availability rules, prevents double-bookings
of rooms, and gives administrators tools for managing rooms, users, and bookings.

This project is built as a portfolio-piece demonstrating full-stack development with **Angular**
(frontend) and **Spring Boot** (backend), backed by **PostgresSQL**, with stateless JWT authentication
and a layered backend architecture.

## 2. Goals and Non-goals

### Goals

- Authenticated users can find and book available conference rooms.
- The system guarantees the core invariant: **no two confirmed bookings of a room should
  overlap**.
- Administrators can manage the room catalog, view all bookings, and make adjustments to
  user roles.
- The application is deployed live and demonstrates clean architectural
  decisions documented in ADRs.

### Non-goals (v1)

The following are deliberately excluded from the first version to keep the scope manageable:

- Payments, billing, or any pricing model.
- Multi-organization or multi-tenant support.
- Recurring bookings (e.g., "every Tuesday at 10am").
- Booking of resources other than rooms (e.g., desks, equipment, parking, etc.).
- External calendar integrations (e.g., Google Calendar, Outlook).
- Real-time push notifications or module apps.
- Waitlisting for unavailable slots.

These are revisited in §12 as candidates for future enhancements.

## 3. Users and Roles

| Role   | Description                                                                                           |
|--------|-------------------------------------------------------------------------------------------------------|
| Guest  | Unauthenticated visitor. Can access only the login and registration pages.                            |
| Member | Authenticated employee, who can browse rooms, create & manage their own bookings.                     |
| Admin  | Has all permissions of a Member, and can also manage rooms, view all bookings, and change user roles. |

Further points:

- A user can have only one role at time.
- THe default role for all new registrations is "MEMBER".
- Role changes are performed by Admin's only.

## 4. Functional Requirements

Numbered for traceability. Each requirement is testable.

### 4.1 Authentication and Account Management

- **FR-1.1** <br>
  A guest can register with email and password. Passwords are
  hashed with BCrypt (strength ≥ 10).
- **FR-1.2** <br>
  A registered user can sign in with email and password and
  receive an access token and refresh token. See ADR-0002.
- **FR-1.3** <br>
  A signed-in user can refresh their access token using the
  refresh token cookie without re-entering credentials.
- **FR-1.4** <br>
  A signed-in user can sign out, which revokes the current
  refresh token.
- **FR-1.5** <br>
  A signed-in user can view their own profile (email, role,
  registration date).
- **FR-1.6** <br>
  Passwords must be at least 8 characters with at least one
  letter and one number. Validation is enforced on both client and server.

### 4.2 Room catalog

- **FR-2.1** <br>
  A member can view a list of all rooms, with name, capacity, location/floor, and a short description.
- **FR-2.2** <br>
  A member can filter rooms by minimum capacity and free-text search across name and description.
- **FR-2.3** <br>
  A member can view a single room's details, including a calendar view of its existing bookings for the
  current and next week.
- **FR-2.4** <br>
  An admin can create a new room with name, capacity, location, and description.
- **FR-2.5** <br>
  An admin can edit a room's metadata.
- **FR-2.6** <br>
  An admin can deactivate a room. Deactivated rooms cannot be booked, but existing future bookings still
  remain visible. Deactivation is soft; rooms are not hard-deleted.

### 4.3 Booking Lifecycle

- **FR-3.1** <br>
  A member can create a booking for an active room by selecting a start time and end time. Bookings must:
    - Start at a 15-minute boundary (`:00`, `:15`, `:30`, `:45`).
    - Be at least 15 minutes long and at most 4 hours long.
    - Start in the future (not in the past).
    - Start no more than 30 days in the future.
- **FR-3.2** <br>
  The system must reject a booking that overlaps any existing booking on the same room, with HTTP 409
  Conflict. See ADR-0001.
- **FR-3.3** <br>
  A member can view their own upcoming and past bookings, sorted by start time descending.
- **FR-3.4** <br>
  A member can cancel one of their own future bookings. Past bookings cannot be canceled.
- **FR-3.5** <br>
  An admin can view all bookings across all rooms and members.
- **FR-3.6** <br>
  An admin can cancel any future booking (e.g., to free a room for maintenance). Cancellation by an admin
  records the actor for audit.
- **FR-3.7** Canceled bookings are soft-deleted <br>(retained with a `cancelled_at` timestamp and `cancelled_by` reference)
  for audit history.

## 4.4 Availability

- **FR-4.1** <br>
  The system will return the appropriate bookings when provided with a room and date, which can then be
  rendered as a day view.
- **FR-4.2** <br>
  When provided with a valid range of dates (within working hours) and a minimum capacity, the system will
  return a list of available rooms, each with at least one time slot satisfying the specified parameters.
- **FR-4.3** <br>
  Default working hours are Mon-Fri, 08:00-18:00, but can be customized the end-user organization.
- **FR-4.4** <br>
  Booking requests outside working hours are rejected outright.

## 4.5 Administration

- **FR-5.1** <br>
  An admin can view a list of all users and their roles.
- **FR-5.2** <br>
  Admins can promote/demote members to/from the admin role. However, admins cannot demote themselves.
- **FR-5.3** <br>
  User accounts can be deactivated by admins. Once deactivated, users can no longer log in, and all their
  pending bookings are automatically canceled.

## 5. Non-Functional Requirements

## 6. Domain Model

### Entities

| **User**                     |
|------------------------------|
| `id` (UUID, PK)              |
| `email` (unique, lowercase)  |
| `password` (BCrypt hash)     |
| `name` (string)              |
| `role` (`MEMBER` \| `ADMIN`) |
| `createdOn` (Date)           |

| **Room**                                 |
|------------------------------------------|
| `id` (UUID, PK)                          |
| `name` (e.g., "Evereest", "Boardroom A") |
| `location` (e.g., "Floor 2, East Wing")  |
| `capacity` (int)                         |
| `amenities` (set of strings)             |
| `isActive` (boolean - soft-delete flag)  |
| `createdOn`                              |

| **Booking**                                                                                                                                                                                   |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `id` (UUID, PL)                                                                                                                                                                               |
| `roomId` (FK → Room)                                                                                                                                                                          |
| `userId` (FK → User)                                                                                                                                                                          |
| `startsAt` (timestamp with timezone)                                                                                                                                                          |
| `endAt` (timestamp with timezone)                                                                                                                                                             |
| `purpose` (short text, optional)                                                                                                                                                              |
| `status` (`CONFIRMED` \| `CANCELLED`)                                                                                                                                                         |
| `cancelledAt`, `cancelledBy` (nullable)                                                                                                                                                       |
| `version` (int — for optimistic locking)                                                                                                                                                      |
| `createdAt`, `updatedAt`                                                                                                                                                                      |
| **Constraint** `Exclude USING gist (room_id WITH =,tstzrange(starts_at, ends_at, '[)') WITH &&) WHERE (cancelled_at IS NULL)` — prevents overlapping non-cancelled bookings on the same room. |

| **RefreshToken**                   |
|------------------------------------|
| `id` (UUID, PK)                    |
| `user` (FK → User, not null)       |
| `tokenHash` (unique, not null)     |
| `expiresAt` (timestamps, not null) |
| `revokedAt` (timestamps, nullable) |

A full ERD is maintained in `docs/erd.png`, and will be regenerated when/if the scheme schanges.

## 7. API surface

REST over HTTPS, JSON request/response and conventional status codes. Full contract is autogenerated as OpenAPI at
`/vs/api/docs` and rendered at `/swagger-ui.html`. This section is a high-level inventory.

### Auth (`/api/auth`)

- `POST /register` ‒ create account, returns user summary
- `POST /login` – exchange credentials for access token (body) + refresh token (cookie)
- `POST /refresh` – exchange refresh token for a new access token
- `POST /logout` – revoke current refresh token
- `GET /me` – return current user profile

### Rooms (`/api/rooms`)

- `GET /` – list rooms (members, admins); supports parameters `?search=` and `?minCapacity=`
- `GET /{id}` – room details (members, admins)
- `GET /{id}/bookings?date=YYYY-MM-DD` – bookings for a room on a given date
- `POST /` – create room (admins only)
- `PUT /{id}` – update room (admins only)
- `DELETE /{id)` – deactivate room (admins only, soft)

### Bookings (`/api/bookings`)

- `GET /me` – current user's bookings (members, admins)
- `POST /` – create booking (members, admins); returns 409 error on conflict
- `DELETE /{id}` – cancel a booking (members can only delete their own bookings; admins can delete any booking)
- `GET /` – list all bookings (admins only); supports filtering

### Users (`/api/users`)

- `GET /` – list all users (admins only)
- `PATCH /{id}/role` – change role of user (admins only; cannot demote self)
- `PATCH /{id}/toggle-active` – activate/deactivate user (admin only)

### Misc

- `GET /api/health` – indicates availability status (public)

### Error Model

All error messages follow the same pattern:

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

| Path                  | Guard  | Purpose                    |
|-----------------------|--------|----------------------------|
| `/`                   | none   | Landing Page               |
| `/login`, `/register` | none   | Auth forms                 |
| `/rooms`              | member | Browse and filter rooms    |
| `/rooms/:id`          | member | Room detail & day calender |
| `/bookings`           | member | current user's bookings    |
| `/book?:roomId`       | member | booking form               |
| `/admin/rooms`        | admin  | manage room inventory      |
| `/admin/bookings`     | admin  | All bookings (filterable)  |
| `/admin/users`        | admin  | manage users               |

### State

As per ADR-0003, a state is a signal residing in an injectable service. The stores are: `AuthStore`, `RoomStore`,
`BookingStore`, `UserStore` (admin only). HTTPP calls return observables, which are stored as signals in the stores.

### Notable UX Behaviors

- On booking conflict (409), the form shows an inline message and refreshes
  the day's bookings so the user sees the now-taken slot.
- A loading spinner is shown for any request taking more than 200 ms.
- Forms use Angular Reactive Forms with custom validators (slot boundary,
  duration limits).

## 9. Design Decisions

Captured as ADRs in `docs/adr/`. Current set:

- **ADR-0001** — Optimistic locking + DB exclusion constraint for booking
  conflict prevention.
- **ADR-0002** — Stateless JWT authentication with split token storage.
- **ADR-0003** — Angular signals for client-side state management.

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
- Concurrency control verified by integration test.
- Day-view calendar on the room detail page.

### Milestone 4 — Admin

- Admin room and user management.
- Admin booking oversight.

### Milestone 5 — Polish

- OpenAPI/Swagger published.
- Test coverage targets met.
- Dockerfile and docker-compose for full local startup.
- README, ADRs, ERD finalized.

## 11. Glossary

- **Booking** — A reservation of a specific room for a specific time range by a specific member.
- **Conflict** — An attempt to create or modify a booking such that it overlaps an existing non-canceled booking on the
  same room.
- **Slot** — A 15-minute boundary at which a booking may start or end.
- **Working hours** — The configured time window during which bookings are permitted.
- **Optimistic locking** — Concurrency control strategy that detects conflicting updates at commit time using a version
  field, rather than holding a lock during the transaction. See ADR-0001.
