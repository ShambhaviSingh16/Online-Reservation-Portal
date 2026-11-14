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
ALTER TABLE booking_events ALTER COLUMN actor_id DROP NOT NULL;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS booking_group_id UUID;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS passenger_name VARCHAR(100);
CREATE INDEX IF NOT EXISTS tickets_booking_group ON tickets(user_id,booking_group_id,ticket_id);

ALTER TABLE users ADD COLUMN IF NOT EXISTS credential_version INTEGER NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS recovery_codes (
 user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
 code_hash VARCHAR(64) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW(),
 PRIMARY KEY(user_id,code_hash)
);

ALTER TABLE journeys ADD COLUMN IF NOT EXISTS advanced_reservations BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE journey_seats ADD COLUMN IF NOT EXISTS travel_class VARCHAR(20) NOT NULL DEFAULT 'STANDARD';
ALTER TABLE journey_seats ADD COLUMN IF NOT EXISTS berth_type VARCHAR(20) NOT NULL DEFAULT 'SEAT';
ALTER TABLE journey_seats ADD COLUMN IF NOT EXISTS rac_enabled BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE IF NOT EXISTS journey_stops (
 journey_id INTEGER NOT NULL REFERENCES journeys(journey_id) ON DELETE CASCADE,
 stop_index INTEGER NOT NULL CHECK(stop_index>=0),station_id INTEGER NOT NULL REFERENCES stations(station_id),
 arrival_time TIMESTAMP NOT NULL,departure_time TIMESTAMP NOT NULL,cumulative_fare NUMERIC(10,2) NOT NULL CHECK(cumulative_fare>=0),
 PRIMARY KEY(journey_id,stop_index),UNIQUE(journey_id,station_id)
);
CREATE TABLE IF NOT EXISTS journey_classes (
 journey_id INTEGER NOT NULL REFERENCES journeys(journey_id) ON DELETE CASCADE,
 travel_class VARCHAR(20) NOT NULL,fare_multiplier NUMERIC(6,2) NOT NULL CHECK(fare_multiplier>0),
 PRIMARY KEY(journey_id,travel_class)
);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS advanced BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS travel_class VARCHAR(20) NOT NULL DEFAULT 'STANDARD';
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS berth_preference VARCHAR(20) NOT NULL DEFAULT 'ANY';
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS segment_start INTEGER;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS segment_end INTEGER;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS allow_rac BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS segment_departure_name VARCHAR(200);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS segment_departure_code VARCHAR(20);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS segment_arrival_name VARCHAR(200);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS segment_arrival_code VARCHAR(20);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS segment_departure_time TIMESTAMP;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS segment_arrival_time TIMESTAMP;
CREATE TABLE IF NOT EXISTS seat_holds (
 hold_id UUID PRIMARY KEY,user_id INTEGER NOT NULL REFERENCES users(user_id),journey_id INTEGER NOT NULL REFERENCES journeys(journey_id) ON DELETE CASCADE,
 segment_start INTEGER NOT NULL,segment_end INTEGER NOT NULL CHECK(segment_end>segment_start),travel_class VARCHAR(20) NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,state VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS seat_holds_expiry ON seat_holds(journey_id,expires_at) WHERE state='ACTIVE';
CREATE TABLE IF NOT EXISTS seat_occupancy (
 journey_id INTEGER NOT NULL,coach_number VARCHAR(20) NOT NULL,seat_number VARCHAR(20) NOT NULL,
 leg INTEGER NOT NULL CHECK(leg>=0),slot INTEGER NOT NULL CHECK(slot IN(0,1)),
 ticket_id INTEGER REFERENCES tickets(ticket_id),hold_id UUID REFERENCES seat_holds(hold_id) ON DELETE CASCADE,
 PRIMARY KEY(journey_id,coach_number,seat_number,leg,slot),
 FOREIGN KEY(journey_id,coach_number,seat_number) REFERENCES journey_seats(journey_id,coach_number,seat_number) ON DELETE CASCADE,
 CHECK((ticket_id IS NULL) <> (hold_id IS NULL))
);
CREATE INDEX IF NOT EXISTS seat_occupancy_ticket ON seat_occupancy(ticket_id);
CREATE INDEX IF NOT EXISTS seat_occupancy_hold ON seat_occupancy(hold_id);
DROP INDEX IF EXISTS tickets_confirmed_seat_unique;
CREATE UNIQUE INDEX tickets_confirmed_seat_unique
 ON tickets(journey_id,UPPER(TRIM(coach_number)),UPPER(TRIM(seat_number))) WHERE status='CONFIRMED' AND NOT advanced;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
 token_hash VARCHAR(64) PRIMARY KEY,user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
 expires_at TIMESTAMPTZ NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE IF NOT EXISTS notification_outbox (
 notification_id BIGSERIAL PRIMARY KEY,event_id BIGINT NOT NULL UNIQUE REFERENCES booking_events(event_id),
 user_id INTEGER NOT NULL REFERENCES users(user_id),ticket_id INTEGER NOT NULL REFERENCES tickets(ticket_id),
 event_type VARCHAR(30) NOT NULL,attempts INTEGER NOT NULL DEFAULT 0,state VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),sent_at TIMESTAMPTZ
);
CREATE OR REPLACE FUNCTION enqueue_booking_notification() RETURNS TRIGGER AS $$
BEGIN
 INSERT INTO notification_outbox(event_id,user_id,ticket_id,event_type)
 SELECT NEW.event_id,t.user_id,t.ticket_id,NEW.action FROM tickets t WHERE t.ticket_id=NEW.ticket_id
 AND NEW.action IN('CONFIRMED','WAITLISTED','RAC','CANCELLED','PROMOTED','PROMOTED_RAC');
 RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS booking_notification_event ON booking_events;
CREATE TRIGGER booking_notification_event AFTER INSERT ON booking_events FOR EACH ROW EXECUTE FUNCTION enqueue_booking_notification();
