# Sangam

Sangam is a social meetup platform where users post events they're attending and invite companions to join them. It provides geospatial discovery, join-request workflows with waitlist auto-promotion, and JWT-secured authentication.

---

## Live demo

| Link | URL |
|------|-----|
| Frontend | https://sangam123.vercel.app |
| Backend API | https://sangam-backend-production.up.railway.app |
| Swagger UI | https://sangam-backend-production.up.railway.app/swagger-ui.html |

**Demo credentials** (seeded automatically on dev startup):

| User | Email | Password |
|------|-------|----------|
| alice | alice@demo.com | password123 |
| bob | bob@demo.com | password123 |
| carol | carol@demo.com | password123 |

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21 · Spring Boot 3.2 · Maven |
| Frontend | React 18 · Vite · Zustand · React Query v5 |
| Database | PostgreSQL 15 · PostGIS 3.3 |
| Cache | Redis 7 |
| Auth | JWT (access 15 min · refresh 7 days, rotated) |
| ORM | JPA/Hibernate Spatial · Flyway migrations |
| Mapping | MapStruct |
| Maps | Leaflet · OpenStreetMap · Nominatim geocoding |
| Prod hosting | Railway (backend + PostGIS) · Vercel (frontend) |
| CI | GitHub Actions |

---

## Local setup

```bash
git clone https://github.com/mars-alien/sangam.git
cd sangam

docker-compose up -d          # starts postgis + redis

cd backend
mvn spring-boot:run           # API at http://localhost:8081
                              # Swagger: http://localhost:8081/swagger-ui.html

cd ../frontend
npm install
npm run dev                   # UI at http://localhost:5173
```

Demo seed data (alice, bob, carol + 5 Bengaluru events) is inserted automatically on first dev startup.

---

## Architecture

```
┌──────────────────┐     HTTPS     ┌──────────────────────────────────┐
│  React SPA       │ ────────────▶ │  Spring Boot REST API            │
│  Vite · Zustand  │               │  Controllers → Services → Repos  │
│  Leaflet maps    │               └──────────┬────────────────────────┘
└──────────────────┘                          │               │
                                              ▼               ▼
                                    ┌──────────────┐  ┌─────────────┐
                                    │  PostgreSQL  │  │    Redis    │
                                    │  + PostGIS   │  │  JWT cache  │
                                    │  geometry    │  │  event list │
                                    └──────────────┘  └─────────────┘
```

**Key flows:**

1. **Discovery** — `GET /api/v1/events?lat=12.97&lng=77.59&radiusKm=10` calls `ST_DWithin` on a spatial index, orders by distance, and returns a cached page.
2. **Join request** — requester sends `POST /events/{id}/join`; if the event is full the request is `WAITLISTED`; when a member leaves, `promoteFromWaitlist` promotes the next person in order.
3. **Auth** — access token (15 min, stateless JWT) + refresh token (7 days, stored hashed in DB, rotated on each use, revoked on logout).

---

## API reference

Swagger UI documents every endpoint with request/response schemas:
- Local: http://localhost:8081/swagger-ui.html
- Prod: https://sangam-backend-production.up.railway.app/swagger-ui.html

---

## What I learned

- **Spatial queries with PostGIS and JTS.** Storing event locations as `geometry(Point, 4326)` and querying with `ST_DWithin` against a `geography` cast gives a radius search that leverages the spatial GiST index instead of a full table scan. Mapping the JTS `Point` type through Hibernate Spatial required learning that JTS uses `(x=longitude, y=latitude)` ordering — the opposite of what most APIs expect — and configuring the `hibernate-spatial` dialect correctly.

- **JWT refresh token rotation.** Short-lived access tokens (15 min) keep the stateless API fast — no DB hit per request. Refresh tokens are stored hashed with SHA-256 and rotated on every use: the old token is deleted before the new one is issued. A stolen token can only be used once before the legitimate client invalidates it. Logout revokes the token by hash.

- **Waitlist auto-promotion.** When a member leaves, the service reindexes the waitlist and promotes the next person in line inside the same transaction. The edge case of multiple departures is handled by computing `available = maxCompanions - currentMemberCount` before touching the waitlist, so exactly the right number of people are promoted.

- **Railway PostGIS deployment.** Railway's managed Postgres does not install PostGIS at the OS level. The fix was deploying a `postgis/postgis:15-3.3` Docker image as a custom Railway service and connecting via private networking (`postgis-db.railway.internal`). Testcontainers uses the same image in the integration test so Flyway migrations run identically in CI and production.

---

## Engineering decisions

### Flyway over Hibernate `ddl-auto`

`ddl-auto: validate` is used in all environments. Schema is owned by versioned Flyway migrations (`V1__`, `V2__`, …). This makes every developer's environment and every CI run reproducible and provides a rollback path. Hibernate `create`/`update` silently diverges across machines and cannot be rolled back.

### PostGIS for geospatial queries

`ST_DWithin(location, ST_MakePoint(:lon, :lat)::geography, :radiusMetres)` leverages a GiST spatial index for O(log n) radius filtering entirely in the database. The alternative — loading all events and filtering by Haversine in Java — does not scale past a few thousand rows.

### Event status state machine

Status transitions (`OPEN → FULL → CANCELLED / COMPLETED`) are enforced in `EventService`, not in the entity or the controller. The service holds the valid transition logic and throws `InvalidOperationException` for illegal moves. Encoding this in JPA lifecycle hooks mixes persistence with business logic; encoding it in the controller violates the thin-controller rule and duplicates logic across endpoints.
