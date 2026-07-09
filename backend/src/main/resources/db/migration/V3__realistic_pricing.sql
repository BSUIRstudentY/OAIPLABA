-- Realistic YouTube-based pricing: on average creators earn about 1 cent per
-- 1000 views. Set CPM to $0.01 / 1000 views and neutralise the watch-time
-- factor so the model pays roughly that realised rate.
UPDATE pricing_config
SET average_cpm = 0.01,
    watch_time_factor = 1.0000
WHERE id = 1;
