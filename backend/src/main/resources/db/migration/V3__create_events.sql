CREATE TABLE events (
    id                   UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    title                VARCHAR(100) NOT NULL,
    description          TEXT         NOT NULL,
    creator_id           UUID         NOT NULL REFERENCES users(id),
    status               VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    category             VARCHAR(20)  NOT NULL,
    location             geometry(Point,4326),
    venue_name           VARCHAR(200) NOT NULL,
    address              VARCHAR(500),
    city                 VARCHAR(100) NOT NULL,
    event_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    event_end_date       TIMESTAMP WITH TIME ZONE,
    min_companions       INT          NOT NULL DEFAULT 1,
    max_companions       INT          NOT NULL,
    current_member_count INT          NOT NULL DEFAULT 0,
    image_url            VARCHAR(500),
    deleted_at           TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE event_tags (
    event_id UUID        NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    tags     VARCHAR(50)
);

CREATE INDEX idx_events_creator  ON events(creator_id);
CREATE INDEX idx_events_status   ON events(status);
CREATE INDEX idx_events_date     ON events(event_date);
CREATE INDEX idx_events_location ON events USING GIST(location);
CREATE INDEX idx_events_fts      ON events
    USING GIN(to_tsvector('english', title || ' ' || coalesce(description, '')));
