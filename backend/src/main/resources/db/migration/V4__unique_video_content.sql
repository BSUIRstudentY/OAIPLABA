-- Prevent the same video file from being uploaded more than once.
-- We store a SHA-256 hash of the file content and enforce global uniqueness.
-- Existing rows keep a NULL hash; Postgres treats NULLs as distinct, so the
-- unique index only constrains newly uploaded videos.
ALTER TABLE videos ADD COLUMN content_hash VARCHAR(64);
CREATE UNIQUE INDEX ux_videos_content_hash ON videos (content_hash);
