# Route segments, classes, selected seats, and RAC

The original single-class booking engine remains available for existing journeys.
An administrator can enable the extended engine on a **future journey with no
existing tickets or seat holds**. The two allocation modes cannot be mixed on one
journey, so legacy reservations keep their original behavior.

## Configure a journey

1. Create the stations and train through the existing admin pages.
2. Create a future journey with its origin, destination, departure/arrival times,
   and base fare. Set the train capacity before creating reservations.
3. Open **Catalog → Configure routes & coaches** (`admin/routePlanner.jsp`).
4. Choose the journey and optionally add intermediate stops in travel order.
5. Enter arrival/departure times and cumulative base fares. The origin starts at
   zero; cumulative fares must strictly increase. First/last stations and times
   must match the journey.
6. Add coaches, choose classes, and set their fare multipliers. Coach labels are
   unique and each coach contains at most 80 seats/berths. Total coach capacity
   cannot exceed the train's capacity.
7. Optionally reserve some side-lower berths for RAC sharing.
8. Save. Once a reservation or hold exists, the route and coach plan cannot be
   replaced. Its base journey schedule is locked to preserve allocations.

The supported demonstration classes are `STANDARD`, `SLEEPER`, `AC3`, and `AC2`.
These are simplified coach models, not official railway inventory or fare rules.
`STANDARD` contains seats and does not support RAC. Other classes contain labeled
berths. The simplified `AC2` model has no middle-berth labels.

## Search and book

Open **Find trains → Explore travel classes, seat maps & intermediate stops**.
Search for any station pair in travel order on the configured route. Date matching
uses the boarding stop's departure date.

Choose the boarding/alighting stops and class. Per-passenger fare is:

```text
(destination cumulative fare - boarding cumulative fare) × class multiplier
```

It is rounded to two decimal places and snapshotted on the ticket. The ticket's
station names and times also reflect the booked segment rather than the entire
train route.

### Automatic allocation and berth preferences

Leave seats unselected for automatic allocation. Preferences are best effort:
the allocator tries the requested berth type and otherwise uses an available
berth in the chosen class. It never silently changes travel class. Group passengers
are booked atomically and do not receive a guarantee of adjacent seats.

### Selected seats and holds

- Select one available full seat/berth per passenger, then choose **Hold selected seats**.
- A hold lasts **120 seconds**, belongs to the authenticated account, and covers
  exactly the selected segment and class.
- Another user cannot consume the hold or reserve overlapping legs of those seats.
- Active holds can be resumed after reloading the seat-map page.
- Booking consumes the hold transactionally; a mismatched passenger count is rejected.
- Expired holds do not silently turn into a different seat selection. Hold seats
  again or explicitly return to automatic allocation.
- Expiry is enforced on the server before inventory operations. Stale occupancy
  rows are removed when availability, booking, release, or cancellation is processed.
- RAC quota berths cannot be selected as exclusive full berths.

### Non-overlapping segment reuse

For route **A → B → C**, the same seat may be booked for **A → B** and **B → C**.
It cannot simultaneously be booked for **A → C**. Occupancy is stored per route
leg; journey-level transactions and a unique leg/seat/slot key prevent overlaps.

The base journey's `available_seats` value represents regular seats free across
the **entire route**, across configured classes. It is not a replacement for the
live class-and-segment seat map.

### RAC and waiting lists

An administrator explicitly reserves side-lower berths as RAC quota. Those berths
are excluded from ordinary exclusive-berth allocation and have two sharing slots.
RAC means a **shared berth**, not an exclusive sleeping berth.

1. The allocator first tries a regular full berth in the chosen class.
2. With explicit RAC consent, it can allocate a free sharing slot in that class.
3. With explicit waiting-list consent, it can create a ticket without an allocation.
4. Without waiting-list consent, insufficient full/RAC capacity rolls back the
   entire group rather than leaving partial reservations.
5. Released full berths are offered to eligible RAC tickets first, then waiting
   tickets, in ticket creation order. Released RAC slots can promote waiting
   passengers who allowed RAC. Incompatible classes/segments are skipped.

Cancellation operates on individual tickets, uses the boarding stop's departure
time, releases only that ticket's occupied legs/slot, and processes promotion in
the same transaction. Historical tickets and audit events remain.

## API entry points

| Method | Endpoint | Purpose |
| --- | --- | --- |
| PUT | `/api/admin/journeys/{id}/plan` | Configure ordered stops and coaches |
| GET | `/api/journeys/{id}/plan` | Read stop/class choices |
| GET | `/api/journeys/segments` | Search station pairs and boarding dates |
| GET | `/api/journeys/{id}/seats` | Current segment/class seat map |
| POST | `/api/bookings/holds` | Hold selected full seats |
| GET | `/api/bookings/holds/active` | Resume your active holds for a journey |
| DELETE | `/api/bookings/holds/{id}` | Release your hold |
| POST | `/api/bookings/advanced` | Atomically reserve named passengers |
| DELETE | `/api/bookings/{id}` | Cancel one ticket through the shared ticket API |
| GET | `/api/bookings/{id}/pdf` | Download your individual ticket |
| GET | `/api/bookings/group/{id}/pdf` | Download your group's passenger tickets |

Swagger describes the request models. Protected endpoints use the existing
session cookie and CSRF token. The original single-ticket/group APIs remain
available for unconfigured single-class journeys.

## External railway integration boundary

`integration/RailwayProvider.java` defines timetable search and capability metadata
for a future authorized provider implementation. The default adapter reports
`configured: false`; `GET /api/journeys/provider` exposes that status. No external
timetable or reservation provider is currently connected, and local demonstration
results are never labeled as live railway data. Provider documentation, authorized
credentials, and contractual access are required before implementing a live adapter.
