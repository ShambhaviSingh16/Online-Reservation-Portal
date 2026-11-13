ALTER TABLE tickets ADD COLUMN IF NOT EXISTS booking_group_id UUID;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS passenger_name VARCHAR(100);
CREATE INDEX IF NOT EXISTS tickets_booking_group ON tickets(user_id,booking_group_id,ticket_id);
