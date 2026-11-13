CREATE TABLE IF NOT EXISTS users (
 user_id SERIAL PRIMARY KEY, username VARCHAR(100) NOT NULL UNIQUE,
 password VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL UNIQUE,
 full_name VARCHAR(200) NOT NULL, role VARCHAR(20) NOT NULL DEFAULT 'user'
);
CREATE TABLE IF NOT EXISTS trains (
 train_id SERIAL PRIMARY KEY, train_number VARCHAR(30) NOT NULL UNIQUE,
 train_name VARCHAR(200) NOT NULL, total_seats INTEGER NOT NULL,
 created_by INTEGER REFERENCES users(user_id)
);
CREATE TABLE IF NOT EXISTS stations (
 station_id SERIAL PRIMARY KEY, station_code VARCHAR(20) NOT NULL UNIQUE,
 station_name VARCHAR(200) NOT NULL, city VARCHAR(100), state VARCHAR(100)
);
CREATE TABLE IF NOT EXISTS journeys (
 journey_id SERIAL PRIMARY KEY, train_id INTEGER NOT NULL REFERENCES trains(train_id),
 departure_station_id INTEGER NOT NULL REFERENCES stations(station_id),
 arrival_station_id INTEGER NOT NULL REFERENCES stations(station_id),
 departure_time TIMESTAMP NOT NULL, arrival_time TIMESTAMP NOT NULL,
 fare NUMERIC(10,2) NOT NULL, available_seats INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS tickets (
 ticket_id SERIAL PRIMARY KEY, journey_id INTEGER NOT NULL REFERENCES journeys(journey_id),
 user_id INTEGER NOT NULL REFERENCES users(user_id), seat_number VARCHAR(20) NOT NULL,
 coach_number VARCHAR(20) NOT NULL, booking_date TIMESTAMP NOT NULL DEFAULT NOW(),
 status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED'
);
-- Fail visibly if older data contains duplicate active reservations. Never delete it automatically.
CREATE UNIQUE INDEX IF NOT EXISTS tickets_confirmed_seat_unique
 ON tickets (journey_id, UPPER(TRIM(coach_number)), UPPER(TRIM(seat_number)))
 WHERE status = 'CONFIRMED';
CREATE TABLE IF NOT EXISTS journey_seats (
 journey_id INTEGER NOT NULL REFERENCES journeys(journey_id) ON DELETE CASCADE,
 position INTEGER NOT NULL, coach_number VARCHAR(20) NOT NULL, seat_number VARCHAR(20) NOT NULL,
 PRIMARY KEY (journey_id, coach_number, seat_number), UNIQUE (journey_id, position)
);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS booking_request_id UUID;
CREATE UNIQUE INDEX IF NOT EXISTS tickets_booking_request_unique
 ON tickets(user_id, booking_request_id) WHERE booking_request_id IS NOT NULL;

ALTER TABLE tickets ALTER COLUMN seat_number DROP NOT NULL;
ALTER TABLE tickets ALTER COLUMN coach_number DROP NOT NULL;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS booked_fare NUMERIC(10,2);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;
UPDATE tickets t SET booked_fare=j.fare FROM journeys j WHERE t.journey_id=j.journey_id AND t.booked_fare IS NULL;
CREATE TABLE IF NOT EXISTS booking_events (
 event_id BIGSERIAL PRIMARY KEY, ticket_id INTEGER NOT NULL REFERENCES tickets(ticket_id),
 actor_id INTEGER NOT NULL REFERENCES users(user_id), action VARCHAR(30) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS tickets_waiting_queue ON tickets(journey_id,ticket_id) WHERE status='WAITLISTED';
CREATE INDEX IF NOT EXISTS tickets_user_history ON tickets(user_id,booking_date DESC,ticket_id DESC);
CREATE INDEX IF NOT EXISTS journeys_search ON journeys(departure_station_id,arrival_station_id,departure_time);

ALTER TABLE trains ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE stations ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
