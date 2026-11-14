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
