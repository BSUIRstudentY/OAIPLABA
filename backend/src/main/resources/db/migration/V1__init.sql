CREATE TABLE users (
    id             UUID PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    display_name   VARCHAR(255) NOT NULL,
    role           VARCHAR(32)  NOT NULL,
    wallet_balance NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL
);

CREATE TABLE videos (
    id                       UUID PRIMARY KEY,
    owner_id                 UUID NOT NULL REFERENCES users (id),
    title                    VARCHAR(255) NOT NULL,
    description              TEXT,
    category                 VARCHAR(64) NOT NULL,
    duration_seconds         INTEGER NOT NULL,
    size_bytes               BIGINT NOT NULL,
    content_type             VARCHAR(128),
    object_key               VARCHAR(512) NOT NULL,
    status                   VARCHAR(32) NOT NULL,
    estimated_monthly_views  BIGINT NOT NULL,
    offer_price              NUMERIC(14, 2) NOT NULL DEFAULT 0,
    offer_breakdown          TEXT,
    created_at               TIMESTAMPTZ NOT NULL,
    resolved_at              TIMESTAMPTZ
);

CREATE INDEX idx_videos_owner ON videos (owner_id);

CREATE TABLE wallet_transactions (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users (id),
    type          VARCHAR(32) NOT NULL,
    amount        NUMERIC(14, 2) NOT NULL,
    balance_after NUMERIC(14, 2) NOT NULL,
    description   VARCHAR(255) NOT NULL,
    video_id      UUID,
    created_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_wallet_tx_user ON wallet_transactions (user_id);

CREATE TABLE pricing_config (
    id                INTEGER PRIMARY KEY,
    average_cpm       NUMERIC(8, 2) NOT NULL,
    watch_time_factor NUMERIC(5, 4) NOT NULL,
    projection_months INTEGER NOT NULL,
    buyout_share      NUMERIC(5, 4) NOT NULL,
    platform_fee      NUMERIC(5, 4) NOT NULL,
    base_monthly_views BIGINT NOT NULL
);

INSERT INTO pricing_config (id, average_cpm, watch_time_factor, projection_months, buyout_share, platform_fee, base_monthly_views)
VALUES (1, 2.50, 0.55, 12, 0.40, 0.10, 5000);
