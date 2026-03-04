# LottoFun

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
| Framework      | Spring Boot 3.x                     |
| Security       | Spring Security + JWT (jjwt 0.11.5) |
| Persistence    | Spring Data JPA / Hibernate         |
| Database       | PostgreSQL 16                       |
| Build Tool     | Maven 3.9                           |
| Containerization | Docker + Docker Compose           |

## Getting Started

### Run with Docker (Recommended)

```bash
git clone https://github.com/cakmakbm/LottoFun.git
cd LottoFun
docker compose up --build
```

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

# 3. Build and run
./mvnw clean package -DskipTests
java -jar target/LottoFun-0.0.1-SNAPSHOT.jar
```

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
  -d '{"username":"admin","password":"admin123"}'

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

28 unit tests covering all service classes (no Spring context, no database required).

## Postman Collection

Import [`postman/LottoFun API.postman_collection.json`](postman/LottoFun%20API.postman_collection.json) into Postman.

The collection includes a `base_url` variable (default: `http://localhost:8080/api/v1`). Login requests automatically save the JWT token to `{{token}}` (user) or `{{admin_token}}` (admin) environment variables, so subsequent requests are authorized without manual copy-paste.

**Recommended flow:** Login (Admin) → Start Draw → Register → Login (User) → Buy Ticket → Execute Draw (Admin) → My Results

## Documentation

See [TECHNICAL_DOCUMENT.md](TECHNICAL_DOCUMENT.md) for full API specification, entity details, draw lifecycle, and architecture diagrams.
