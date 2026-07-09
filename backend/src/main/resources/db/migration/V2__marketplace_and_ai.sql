-- Peer-to-peer marketplace fields
ALTER TABLE videos ADD COLUMN listed_for_sale BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE videos ADD COLUMN sale_price NUMERIC(14, 2);
ALTER TABLE videos ADD COLUMN listed_at TIMESTAMPTZ;

-- AI valuation fields
ALTER TABLE videos ADD COLUMN ai_fair_price NUMERIC(14, 2);
ALTER TABLE videos ADD COLUMN ai_analysis TEXT;

CREATE INDEX idx_videos_listed ON videos (listed_for_sale);
