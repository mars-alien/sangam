# GatherUp

GatherUp is a social meetup platform where users post events they're attending and invite companions to join them. It provides geospatial discovery, join-request workflows with waitlist auto-promotion, and JWT-secured authentication.

---

## Tech stack

| Layer        | Technology                              |
|--------------|-----------------------------------------|
| Backend      | Java 21 · Spring Boot 3.2 · Maven       |
| Frontend     | React 18 · Vite · JavaScript            |
| Database     | PostgreSQL 15 · PostGIS 3.3             |
| Cache        | Redis 7                                 |
| Auth         | JWT (access 15 min · refresh 7 days)    |
| ORM          | JPA/Hibernate · Flyway                  |
| Mapping      | MapStruct                               |
| Dev infra    | Docker Compose                          |
| Prod hosting | Railway                                 |

---

## Local setup

```bash
git clone https://github.com/your-org/gatherup.git
cd gatherup

cp .env.example .env          # edit JWT_SECRET at minimum

docker-compose up -d          # starts postgres + redis

cd backend
mvn spring-boot:run           # API at http://localhost:8080

cd ../frontend
npm install
npm run dev                   # UI at http://localhost:5173
```

**API docs:** http://localhost:8080/swagger-ui.html

---

## Architecture

```
┌─────────────┐     ┌──────────────────────────────────┐     ┌────────────┐
│  React SPA  │────▶│  Spring Boot REST API             │────▶│ PostgreSQL │
│  Vite dev   │     │  Controllers → Services → Repos   │     │ + PostGIS  │
└─────────────┘     └────────────────┬─────────────────┘     └────────────┘
                                     │
                                     ▼
                              ┌─────────────┐
                              │    Redis    │
                              │ JWT blacklist│
                              │ event cache │
                              └─────────────┘
```

---

## Engineering decisions

### PostGIS for geospatial queries

Events store a `geometry(Point, 4326)` column. Discovery uses `ST_DWithin(location, ST_MakePoint(:lon, :lat)::geography, :radiusMetres)`, which leverages the spatial index for O(log n) radius filtering entirely inside the database. The alternative — fetching all events and filtering by Haversine in Java — would require loading the full table on every search request and would not scale past a few thousand events.

### JWT refresh token rotation

Access tokens are short-lived (15 min) and stateless — the API validates them by signature alone with no database round-trip on every request. Refresh tokens are long-lived (7 days), stored in PostgreSQL, and rotated on each use: the old token is deleted and a new one issued. This means a stolen refresh token can only be used once before the legitimate client invalidates it on next refresh. Logout explicitly deletes the refresh token; access tokens are blacklisted in Redis for their remaining TTL.

### Flyway over Hibernate DDL auto

`ddl-auto: validate` is used in all environments. All schema is managed by versioned Flyway migrations (`V1__`, `V2__`, …). This gives every developer and every CI run a reproducible, auditable schema history. Hibernate `create`/`update` is safe in a solo prototype but silently diverges across machines and cannot be rolled back, which causes hard-to-diagnose failures in team and production environments.

### Event status state machine

Status transitions (`DRAFT → OPEN → FULL → CANCELLED / EXPIRED`) are enforced in `EventService`, not in the entity or the controller. The service holds the valid transition map and throws `BadRequestException` for illegal moves. Encoding this at the entity level with JPA lifecycle hooks mixes persistence concerns with business rules; encoding it in the controller violates the thin-controller rule and duplicates logic across endpoints.
