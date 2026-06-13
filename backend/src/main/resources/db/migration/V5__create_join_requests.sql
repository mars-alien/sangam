CREATE TABLE join_requests (
    id                UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id          UUID        NOT NULL REFERENCES events(id)     ON DELETE CASCADE,
    requester_id      UUID        NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message           VARCHAR(300),
    waitlist_position INT,
    responded_at      TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(event_id, requester_id)
);

CREATE INDEX idx_join_requests_event     ON join_requests(event_id);
CREATE INDEX idx_join_requests_requester ON join_requests(requester_id);
CREATE INDEX idx_join_requests_status    ON join_requests(event_id, status);
