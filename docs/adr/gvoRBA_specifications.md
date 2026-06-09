# gvoRBA Platform - Sepcification

**Version:** 0.1 (Draft)
**Status:** Living document — updated as decisions are made
**Last updated:** 2026-06-08

---

## 1. Overview

This is a web application that lets employees within an organization browse for and book
conference rooms for meetings. The system enforces availabilty rules, prevents double-bookings
of rooms, and gives administrators tools for managing rooms, users, and bookings.

This project is built as a portfolio-piece demonstating full-stack development with **Angular**
(frontend) and **Spring Boot** (backend), backed by **MySQL**, with stateless JWT authentication 
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
- Multi-organization or mutli-tenant support.
- Recurring bookings (e.g., "every Tuesday at 10am").
- Booking of resources other than rooms (e.g., desks, equipment, parking, etc.).
- External calendar integrations (e.g., Google Calendar, Outlook).
- Real-time push notifications or modile apps.
- Waitlisting for unavailable slots.

These are revisited in §12 as candidates for future enhancements.

##3. Users and Roles

| Role | Description |
|------|-------------|
| Guest | Unauthenticated visitor. Can access only the login and registration pages. |
| Member | Authenticated employee, who can browse rooms, create & manage their own bookings. |
| Admin | Has all permissions of a Member, and can also manage rooms, view all bookings, and change user roles. |

Further points:
- A user can have only one role at time. 
- THe default role for all new registrations is "MEMBER".
- Role changes are performed by Admin's only.

## 4. Domain Model

### Entities

| **User** |
|----------|
| `id` (UUID, PK) |
| `email` (unique, lowercase) |
| `password` (BCrypt hash) |
| `name` (string) |
| `role` (`MEMBER` \| `ADMIN`) |
| `createdOn` (Date) |

| **Room** |
|----------|
| `id` (UUID, PK) |
| `name` (e.g., "Evereest", "Boardroom A") |
| `location` (e.g., "Floor 2, East Wing") |
| `capacity` (int) |
| `amenities` (set of strings) |
| `isActive` (boolean - soft-delete flag) |
| `createdOn` |

| **Booking** |
|-------------|
| `id` (UUID, PL) |
| `roomId` (FK → Room) |
| `userId` (FK → User) |
| `startsAt` (timestamp with timezone) |
| `endAt` (timestamp with timezone) |
| `purpose` (short text, optional) |
| `status` (`CONFIRMED` \| `CANCELLED`) |
| `cancelledAt`, `cancelledBy` (nullable) |
| `createdOn` |
| `version` (int — for optimistic locking) |

### Relationships

- `User 1—N Booking`
- `Room 1—N Booking`

### Database-Level Invariant


