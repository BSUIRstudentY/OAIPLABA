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
