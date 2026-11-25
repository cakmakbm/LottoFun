# LottoFun

[![CI/CD](https://github.com/cakmakbm/LottoFun/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/cakmakbm/LottoFun/actions/workflows/ci-cd.yml)

A RESTful lottery backend built with Spring Boot. Users register, purchase tickets for open draws, and receive prizes automatically when the draw is executed.

## Features

- JWT-based authentication (register / login)
- Role-based access control — `ROLE_USER` and `ROLE_ADMIN`
- Admin account auto-created on first startup
- Buy lottery tickets for the active draw
- Automatic draw execution via scheduler (every 60 seconds)
- Manual draw execution via admin endpoint
- Prize distribution with 4 categories (JACKPOT / HIGH / MEDIUM / NO_PRIZE)
- Draw history with winner counts per category

## Tech Stack

| Layer          | Technology                          |
|----------------|-------------------------------------|
| Language       | Java 17                             |
| Framework      | Spring Boot 4.x                     |
| Security       | Spring Security + JWT (jjwt 0.11.5) |
| Persistence    | Spring Data JPA / Hibernate         |
| Database       | PostgreSQL 16                       |
| Build Tool     | Maven 3.9                           |
| Containerization | Docker + Docker Compose           |
| Orchestration  | Kubernetes (Kustomize), kind for local and CI |

## Getting Started

### Run with Docker (Recommended)

```bash
git clone https://github.com/cakmakbm/LottoFun.git
cd LottoFun

# Create your local secrets file (it is git-ignored) and fill in the values
cp .env.example .env
#   POSTGRES_PASSWORD=<any password>
#   JWT_SECRET=<output of: openssl rand -base64 32>
#   ADMIN_PASSWORD=<password for the admin account>

docker compose up --build
```

No secrets are stored in the repository. The app refuses to start without `JWT_SECRET`, and the admin account is only created when `ADMIN_PASSWORD` is set.

The application will be available at `http://localhost:8080`.

To stop:
```bash
docker compose down

# Also remove the database volume (clean slate):
docker compose down -v
```

### Run Locally

Requires a running PostgreSQL instance.

```bash
# 1. Create a database named "lottofun"

# 2. Set connection details in src/main/resources/application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/lottofun
spring.datasource.username=your_user
spring.datasource.password=your_password

# 3. Provide secrets as environment variables
export JWT_SECRET=$(openssl rand -base64 32)
export ADMIN_PASSWORD=<password for the admin account>

# 4. Build and run
./mvnw clean package -DskipTests
java -jar target/LottoFun-0.0.1-SNAPSHOT.jar
```

### Run on Kubernetes

Manifests live in [`k8s/`](k8s) and are managed with Kustomize (built into `kubectl`):

| Resource | Purpose |
|----------|---------|
| `StatefulSet` `postgres` + headless `Service` | PostgreSQL 16 with a 1 Gi persistent volume |
| `Deployment` `lottofun` (2 replicas) + `Service` | The API, rolled out with `maxUnavailable: 0` |
| `ConfigMap` / `Secret` (generated) | Lottery rules and datasource settings / DB password, JWT secret, admin password |
| `PodDisruptionBudget` | Keeps at least one API pod running during node maintenance |

Each API pod has:

- **Probes**: a startup probe, a liveness probe at `/actuator/health/liveness`, and a readiness probe at `/actuator/health/readiness`. The readiness probe includes the database, so a pod that cannot reach PostgreSQL gets no traffic.
- **An init container** that waits for PostgreSQL to accept connections.
- **Graceful shutdown**: a `preStop` delay and Spring's graceful shutdown, so in-flight requests finish.
- **Hardening**: it runs as a non-root user with a read-only root filesystem, no privilege escalation, and all Linux capabilities dropped.
- **Resource requests and limits**, with the JVM heap sized from the memory limit.

Running two replicas is safe because both run the `@Scheduled` draw check, but a draw is claimed with one conditional `UPDATE`, so only one replica can execute it.

Local cluster with [kind](https://kind.sigs.k8s.io/):

```bash
kind create cluster --name lottofun

# Build the image and load it into the cluster (no registry needed)
docker build -t ghcr.io/cakmakbm/lottofun:local .
kind load docker-image ghcr.io/cakmakbm/lottofun:local --name lottofun

# Secrets (git-ignored)
cp k8s/base/secrets.env.example k8s/base/secrets.env
#   POSTGRES_PASSWORD=<any password>
#   JWT_SECRET=<output of: openssl rand -base64 32>
#   ADMIN_PASSWORD=<password for the admin account>

kubectl apply -k k8s/overlays/kind
kubectl -n lottofun rollout status deployment/lottofun

# End-to-end check through the Service
./k8s/smoke-test.sh

# Use the API at http://localhost:8080
kubectl -n lottofun port-forward svc/lottofun 8080:80

# Clean up
kind delete cluster --name lottofun
```

`k8s/base` uses `ghcr.io/cakmakbm/lottofun:latest` from GitHub Container Registry; `k8s/overlays/kind` swaps in the locally built `:local` image.

## API Overview

Base URL: `http://localhost:8080/api/v1`

All endpoints except `/auth/**` require:
```
Authorization: Bearer <jwt_token>
```

| Method | Endpoint              | Role       | Description                  |
|--------|-----------------------|------------|------------------------------|
| POST   | `/auth/register`      | Public     | Register and get JWT          |
| POST   | `/auth/login`         | Public     | Login and get JWT             |
| POST   | `/tickets/buy`        | User       | Buy a ticket for active draw  |
| GET    | `/tickets/my-results` | User       | View own tickets and results  |
| POST   | `/draws/start`        | Admin only | Create a new draw             |
| POST   | `/draws/execute`      | Admin only | Manually execute active draw  |
| GET    | `/draws/history`      | User       | View completed draws          |

### Quick Start Flow

```bash
# 1. Login as admin (created automatically on startup)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<your ADMIN_PASSWORD>"}'

ADMIN_TOKEN="<token from response>"

# 2. Start a draw
curl -X POST http://localhost:8080/api/v1/draws/start \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 3. Register a user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"pass123"}'

USER_TOKEN="<token from response>"

# 4. Buy a ticket
curl -X POST http://localhost:8080/api/v1/tickets/buy \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedNumbers":[7,14,21,35,49]}'

# 5. Execute the draw (admin)
curl -X POST http://localhost:8080/api/v1/draws/execute \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 6. Check results
curl http://localhost:8080/api/v1/tickets/my-results \
  -H "Authorization: Bearer $USER_TOKEN"
```

## Lottery Rules

| Rule                 | Value                        |
|----------------------|------------------------------|
| Numbers to pick      | 5                            |
| Valid range          | 1 – 49                       |
| Ticket price         | 10.00                        |
| Initial user balance | 100.00 (10 free tickets)     |
| Draw frequency       | Every 7 days                 |
| Scheduler poll rate  | Every 60 seconds             |

## Prize Categories

| Category  | Matches | Prize         |
|-----------|:-------:|---------------|
| JACKPOT   | 5       | 100,000.00    |
| HIGH      | 4       | 5,000.00      |
| MEDIUM    | 3       | 250.00        |
| NO_PRIZE  | ≤ 2     | 0.00          |

## Running Tests

```bash
./mvnw test
```

- **31 unit tests** covering all service classes and the global exception handler (no Spring context, no database required).
- **2 health endpoint tests** checking that the Kubernetes probes are public and no other actuator endpoint is.
- **2 concurrency integration tests** on an in-memory H2 database:
  - 20 simultaneous purchases at one account: the balance is never overdrawn and no deduction is lost.
  - 10 simultaneous executions of the same draw through the real `@Async` executor: the draw runs exactly once and exactly one next draw is created.

## CI/CD

GitHub Actions ([`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml)) runs on every push and pull request:

1. **Build and test**: compiles with Java 17 and runs all tests with `./mvnw verify`. Test reports are uploaded as a build artifact.
2. **Deploy to Kubernetes (kind)**: creates a kind cluster, builds and loads the image, applies `k8s/overlays/kind`, waits for both rollouts and runs [`k8s/smoke-test.sh`](k8s/smoke-test.sh) against the deployed Service.
3. **Publish Docker image**: only on `main`, and only after the first two jobs pass, builds the Docker image and pushes it to GitHub Container Registry as `ghcr.io/cakmakbm/lottofun:latest` and a commit-SHA tag.

```bash
docker pull ghcr.io/cakmakbm/lottofun:latest
```

## Postman Collection

Import [`postman/LottoFun API.postman_collection.json`](postman/LottoFun%20API.postman_collection.json) into Postman.

The collection includes a `base_url` variable (default: `http://localhost:8080/api/v1`). Login requests automatically save the JWT token to `{{token}}` (user) or `{{admin_token}}` (admin) environment variables, so subsequent requests are authorized without manual copy-paste.

**Recommended flow:** Login (Admin) → Start Draw → Register → Login (User) → Buy Ticket → Execute Draw (Admin) → My Results

## Documentation

See [TECHNICAL_DOCUMENT.md](TECHNICAL_DOCUMENT.md) for full API specification, entity details, draw lifecycle, and architecture diagrams.
