# VidVault

**VidVault** is a video buyout marketplace. Creators upload a video; the platform
instantly estimates how much it could earn through YouTube monetisation and offers
to buy it outright. When the author accepts, the payout is credited to their wallet.

## Stack

| Layer     | Technology |
|-----------|------------|
| Frontend  | React 18 + Vite + TypeScript + Tailwind CSS (dark, "Vibrant & Block-based" design system from [ui-ux-pro-max-skill](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill)) |
| Backend   | Java 21 + Spring Boot 3 (Web, Security, Data JPA, Validation, Actuator) |
| Auth      | JWT access + refresh tokens, BCrypt password hashing, role-based access (USER / ADMIN) |
| Database  | PostgreSQL 16 (schema managed by Flyway) |
| Storage   | MinIO (S3-compatible) for video files, with presigned download URLs |
| Runtime   | Docker Compose orchestrates everything |

## Quick start

```bash
cp .env.example .env        # optional; defaults work out of the box
docker compose up --build   # builds and starts db, minio, backend, frontend
```

Then open:

| URL | What |
|-----|------|
| http://localhost:8081 | Web app (frontend) |
| http://localhost:8080/swagger-ui.html | API docs (Swagger UI) |
| http://localhost:8080/actuator/health | Backend health |
| http://localhost:9001 | MinIO console (`minioadmin` / `minioadmin`) |

A default admin account is seeded: **admin@vidvault.io** / **admin12345**.

## Features

- **Auth:** register / login / refresh, roles USER & ADMIN, seeded admin.
- **Upload & platform buyout:** upload a video; the platform makes an instant buyout offer from projected YouTube monetisation; accept to get paid.
- **AI valuation engine:** on upload the backend runs ffprobe + ffmpeg scene detection on the actual file to extract real signals (resolution, fps, bitrate, audio, scene changes) and produces a **fair, deliberately conservative price** plus a rationale ("why this price"), context tags, and **key-moment timecodes**. It anchors on projected revenue and never over-values a clip.
- **Peer-to-peer marketplace:** owners list videos for sale at their own price; other users browse and buy. Buying transfers ownership and moves funds between wallets atomically.
- **Wallet:** simulated deposit and withdrawal (prototype, no real acquiring), with a full signed transaction history (deposit, buyout, sale, purchase, withdrawal).
- **Admin panel:** edit the pricing model and review all uploads.

## Key API endpoints

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/auth/{register,login,refresh}` | Authentication |
| POST | `/api/videos` (multipart) | Upload video → platform offer + AI valuation |
| GET  | `/api/videos/{mine,{id}}` | List own / get one |
| POST | `/api/videos/{id}/{accept-offer,reject-offer}` | Platform buyout |
| POST | `/api/videos/{id}/{list,unlist}` | List / unlist on the marketplace |
| GET  | `/api/marketplace` · POST `/api/marketplace/{id}/buy` | Browse / buy |
| GET  | `/api/wallet` · POST `/api/wallet/{deposit,withdraw}` | Wallet |
| GET/PUT | `/api/admin/pricing-config` · GET `/api/admin/videos` | Admin |

## How the buyout price is calculated

The offer is fully transparent and configurable by admins (Admin panel → Pricing model):

```
estimated monthly views = base views x category multiplier x duration factor
monthly revenue         = views x CPM / 1000 x watch-time factor
projected revenue       = monthly revenue x projection months
gross offer             = projected revenue x buyout share
offer price             = gross offer x (1 - platform fee)
```

Authors can also provide their own expected monthly views to override the estimate.

## Project layout

```
backend/    Spring Boot API (Maven)
frontend/   React SPA (Vite) served by nginx in Docker
infra/      (reserved for future infra config)
docker-compose.yml
```

## Local development (without Docker)

- Backend: `cd backend && mvn spring-boot:run` (needs Postgres + MinIO reachable; see `application.yml` env vars).
- Frontend: `cd frontend && npm install && npm run dev` (proxies `/api` to `http://localhost:8080`).
