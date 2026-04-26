# Setup Guide

## Quick Start (Docker — recommended)

The fastest way to run the full stack is `docker-start.sh`. It builds and starts every service, waits for the API to become healthy, fetches an operator API key from the seed data, and writes it into `apps/web/.env` automatically.

### 1. Create `.env` at the repo root

Copy the example and fill in the admin credentials:

```sh
cp .env.example .env
```

The file only needs two values:

```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your-admin-password-here
```

> The script validates that both are set before doing anything. The `ADMIN_USERNAME` / `ADMIN_PASSWORD` you enter here become the HTTP Basic auth credentials for all `/admin/*` endpoints.

### 2. Run the start script

```sh
./docker-start.sh
```

What the script does, step by step:

| Step | Action |
|------|--------|
| 1 | Loads `.env` into the shell environment |
| 2 | Builds and starts `postgres`, `redis`, `api`, `grafana` |
| 3 | Polls `GET /actuator/health` until the API responds healthy |
| 4 | Calls `GET /api/v1/admin/api-keys?size=1` with Basic auth |
| 5 | Parses the first `keyHash` from the response |
| 6 | Writes `API_KEY="<value>"` into `apps/web/.env` |
| 7 | Builds and starts the `web` container |
| 8 | Prints `docker compose ps` so you can see all running services |

After the script completes:

| Service | URL |
|---------|-----|
| Next.js dashboard | http://localhost:3000 |
| Spring Boot API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Grafana | http://localhost:4000 (admin / admin) |

---

## Manual Local Development

Use this approach if you want hot-reloading in the web app or the API.

### Prerequisites

- Node.js ≥ 18, pnpm 9
- Java 17 + Maven (or use the included `./mvnw` wrapper)
- Docker (for infrastructure)

### 1. Start infrastructure only

```sh
docker compose up -d postgres redis
```

### 2. Start the API

```sh
cd apps/api
./mvnw spring-boot:run
```

The API starts on port **8081**. Flyway runs migrations automatically on startup.

### 3. Configure and start the web app

Create `apps/web/.env.local`:

```env
API_URL=http://localhost:8081/api/v1
API_KEY=<operator-api-key>
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your-admin-password
NEXT_PUBLIC_TAP_DEBOUNCE_MS=1000
```

To get an operator API key, call the admin endpoint (after the API is running):

```sh
curl -s -u admin:your-admin-password \
  http://localhost:8081/api/v1/admin/api-keys?size=1 \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['content'][0]['keyHash'])"
```

Then start the web app:

```sh
cd apps/web
pnpm dev       # http://localhost:3000
```

### Run everything at once (Turborepo)

```sh
pnpm dev       # from repo root
```

> This starts the API and web app concurrently but does not start infrastructure — make sure Docker is running first.

---

## Environment Variable Reference

### Root `.env` (used by `docker-start.sh` and Docker Compose)

| Variable | Required | Description |
|----------|----------|-------------|
| `ADMIN_USERNAME` | Yes | Basic auth username for `/admin/*` endpoints |
| `ADMIN_PASSWORD` | Yes | Basic auth password for `/admin/*` endpoints |
| `API_KEY` | Auto-set | Written by `docker-start.sh`; passed to the web container |
| `API_URL` | Optional | Overrides the API base URL (default: `http://api:8081/api/v1`) |

### `apps/web/.env.local` (local dev only)

| Variable | Description |
|----------|-------------|
| `API_URL` | Spring Boot base URL, e.g. `http://localhost:8081/api/v1` |
| `API_KEY` | Operator API key (`keyHash` from the `api_keys` table) |
| `ADMIN_USERNAME` | Basic auth username |
| `ADMIN_PASSWORD` | Basic auth password |
| `NEXT_PUBLIC_TAP_DEBOUNCE_MS` | Cooldown between tap button presses in the simulator (default: `1000`) |

### Spring Boot API (set via environment or `application.yml`)

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/littletrip` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Database password |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
