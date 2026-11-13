-- Automatic promotion has no human actor. Existing history is retained.
ALTER TABLE booking_events ALTER COLUMN actor_id DROP NOT NULL;
