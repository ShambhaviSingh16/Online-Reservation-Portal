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
