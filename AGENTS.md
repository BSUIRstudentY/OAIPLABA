# AGENTS.md

## Cursor Cloud specific instructions

### What this project is
**VidVault** is a video buyout marketplace: creators upload videos, the backend
estimates their YouTube monetisation potential and makes an instant buyout offer,
and accepting an offer credits the payout to the author's wallet. Monorepo:

- `backend/` — Java 21 + Spring Boot 3 REST API (Maven). Auth = JWT access+refresh,
  BCrypt, roles USER/ADMIN. Persistence = PostgreSQL via **Flyway** (schema is
  Flyway-owned; JPA `ddl-auto=none`). Video files live in **MinIO** (S3-compatible).
- `frontend/` — React 18 + Vite + TypeScript + Tailwind, served by nginx in Docker.
- `docker-compose.yml` — orchestrates `db`, `minio`, `backend`, `frontend`.

Standard commands and URLs are documented in `README.md`; prefer it over duplicating here.

### Running the stack (the intended way)
`docker compose up --build` brings up everything. Ports: frontend `8081`, backend
`8080`, MinIO API `9000` / console `9001`, Postgres `5432`. Seeded admin:
`admin@vidvault.io` / `admin12345`.

### Non-obvious caveats (read before running)
- **Docker needs `sudo` and a running daemon.** This VM has no systemd, so the
  Docker daemon is not auto-started. If `docker ps` fails, start it once with
  `sudo bash -c 'nohup dockerd >/var/log/dockerd.log 2>&1 &'` and use `sudo docker`
  / `sudo docker compose`. Docker 29 here uses the `fuse-overlayfs` storage driver
  (configured in `/etc/docker/daemon.json`); do not switch it to the containerd
  snapshotter or image builds break in this environment.
- **MinIO presigned download URLs are signed against `MINIO_PUBLIC_ENDPOINT`**
  (default `http://localhost:9000`), not the internal `minio:9000` hostname. The
  backend uses a separate presign-only MinIO client for this
  (`MinioConfig.minioPresignClient`); do NOT "fix" it to reuse the internal client
  or the SigV4 signature will not match the URL the browser requests.
- **Startup ordering:** the backend retries the MinIO bucket check on boot and
  before each upload, and waits for Postgres via the compose healthcheck. The
  `minio/minio` image ships no `curl`/`wget`, so it has no compose healthcheck by
  design — rely on the backend's retry logic rather than adding one.
- **Frontend production build** is `tsc && vite build` (see `frontend/package.json`).
  It does not use `tsc -b` project references (that conflicted with `noEmit`).
- **AI valuation needs ffmpeg/ffprobe in the backend container** — they are
  installed in `backend/Dockerfile` (runtime stage) and power
  `VideoAnalysisService` (scene detection → key-moment timecodes, probing). If
  they are missing the engine degrades gracefully to a metadata-only fallback
  (lower confidence) rather than failing the upload. Analysis runs synchronously
  during upload and is bounded (probes first ~3 min of video) with timeouts.
- P2P purchases move funds and transfer ownership in one transaction and set the
  video's platform-buyout `status` to `REJECTED` for the new owner (so they can't
  accept a stale platform offer); marketplace/wallet balances can never go negative.
- Local (non-Docker) dev: backend `cd backend && mvn spring-boot:run` (needs
  Postgres+MinIO reachable); frontend `cd frontend && npm run dev` (Vite proxies
  `/api` to `http://localhost:8080`). Lint: `cd frontend && npm run lint`.
