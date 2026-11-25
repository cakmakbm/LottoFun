# LottoFun — Technical Document

## Table of Contents
1. [Architecture & Design](#1-architecture--design)
2. [Domain Model Design](#2-domain-model-design)
3. [Draw Lifecycle & Prize Distribution](#3-draw-lifecycle--prize-distribution)
4. [API Specification](#4-api-specification)
5. [Setup Instructions](#5-setup-instructions)
6. [Testing with Postman](#6-testing-with-postman)
7. [Testing Approach](#7-testing-approach)

---

## 1. Architecture & Design

### 1.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          CLIENT                                  │
│                  (REST API Consumer / Postman)                   │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTPS / JSON
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                     SPRING BOOT APPLICATION                      │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │                  Security Layer                           │   │
│  │   JwtAuthenticationFilter → CustomUserDetailsService      │   │
│  └──────────────────────────────────────────────────────────┘    │
│                            │                                     │
│  ┌─────────────────────────▼────────────────────────────────┐    │
│  │                  Controller Layer                         │   │
│  │   AuthController  │  TicketController  │  DrawController  │   │
│  └─────────────────────────┬────────────────────────────────┘    │
│                            │                                     │
│  ┌─────────────────────────▼────────────────────────────────┐    │
│  │                   Service Layer                           │   │
│  │                                                           │   │
│  │  AuthService      UserService      TicketService          │   │
│  │  DrawService      DrawExecutionService  (async)           │   │
│  │  PrizeDistributionService  NumberGeneratorService         │   │
│  │  TicketValidator  AuthHelper                              │   │
│  └─────────────────────────┬────────────────────────────────┘    │
│                            │                                     │
│  ┌─────────────────────────▼────────────────────────────────┐    │
│  │                 Repository Layer                          │   │
│  │        UserRepository  DrawRepository  TicketRepository   │   │
│  └─────────────────────────┬────────────────────────────────┘    │
│                            │                                     │
└────────────────────────────┼────────────────────────────────────┘
                             │ JPA / Hibernate
                             ▼
                  ┌─────────────────────┐
                  │    PostgreSQL DB    │
                  │                     │
                  │  users              │
                  │  draws              │
                  │  tickets            │
                  │  draw_winning_nums  │
                  │  ticket_numbers     │
                  └─────────────────────┘
```

### 1.2 Technology Stack

| Layer            | Technology                          |
|------------------|-------------------------------------|
| Language         | Java 17                             |
| Framework        | Spring Boot 4.x                  |
| Security         | Spring Security + JWT (jjwt 0.11.5) |
| Persistence      | Spring Data JPA / Hibernate         |
| Database         | PostgreSQL 16                       |
| Build Tool       | Maven 3.9                           |
| Boilerplate      | Lombok                              |
| Testing          | JUnit 5, Mockito, AssertJ           |
| Containerization | Docker + Docker Compose             |

### 1.3 Package Structure

```
com.bmc.lottofun
├── config/              # Spring beans, security filter chain
├── controller/          # REST endpoints (thin layer, no business logic)
├── dto/
│   ├── request/         # Incoming request DTOs
│   └── response/        # Outgoing response DTOs
├── entity/              # JPA entities
│   └── enums/           # DrawStatus, TicketStatus
├── exception/           # Custom exception hierarchy + GlobalExceptionHandler
├── repository/          # Spring Data JPA interfaces
├── security/            # JWT filter, CustomUserDetails, AuthHelper
└── service/
    ├── interfaces/      # Service contracts (DIP)
    └── *.java           # Implementations
```

### 1.4 Design Principles Applied

| Principle | Implementation |
|-----------|---------------|
| **SRP** | `TicketValidator` validates only; `UserService` handles balance only; `DrawExecutionService` executes only |
| **OCP** | Custom exception hierarchy extends `BusinessException`; new types require no handler changes |
| **DIP** | Controllers and services depend on interfaces (`ITicketService`, `IUserService`, etc.) |
| **Layered** | Controller → Service (interface) → Repository; no layer skipping |
| **Security** | `User` entity is a plain domain object; `CustomUserDetails` adapts it for Spring Security |

---

## 2. Domain Model Design

### 2.1 Entity Relationship Diagram

```
┌──────────────────────┐         ┌──────────────────────────┐
│         User         │         │          Draw            │
├──────────────────────┤         ├──────────────────────────┤
│ PK  id        BIGINT │         │ PK  id          BIGINT   │
│     username  VARCHAR│         │     date        TIMESTAMP│
│     password  VARCHAR│         │     status      VARCHAR  │
│     balance   DECIMAL│         │                          │
│     role      VARCHAR│         │                          │
└──────────┬───────────┘         └───────────┬──────────────┘
           │                                 │
           │ 1                               │ 1
           │          ┌──────────────────────┴─────────────────┐
           │          │             Ticket                      │
           │          ├─────────────────────────────────────────┤
           └──────────┤ PK  id            BIGINT                │
              N       │ FK  user_id       BIGINT                │
                      │ FK  draw_id       BIGINT                │
                      │     status        VARCHAR               │
                      │     purchase_time TIMESTAMP             │
                      │     match_count   INT                   │
                      │     prize_amount  DECIMAL               │
                      └──────────────┬──────────────────────────┘
                                     │
              ┌──────────────────────┴──────────────────────────┐
              │                                                  │
    ┌─────────▼──────────┐                     ┌────────────────▼──────┐
    │   ticket_numbers   │                     │  draw_winning_numbers │
    ├────────────────────┤                     ├───────────────────────┤
    │ FK ticket_id       │                     │ FK draw_id            │
    │    number  INT     │                     │    number  INT        │
    └────────────────────┘                     └───────────────────────┘
```

### 2.2 Entity Details

#### User
| Field    | Type           | Constraints             | Description                        |
|----------|----------------|-------------------------|------------------------------------|
| id       | BIGINT         | PK, AUTO_INCREMENT      | Unique identifier                  |
| username | VARCHAR        | UNIQUE, NOT NULL        | Login name                         |
| password | VARCHAR        | NOT NULL                | BCrypt hashed                      |
| balance  | DECIMAL(19,2)  | NOT NULL                | Current wallet balance             |
| role     | VARCHAR        | NOT NULL                | `ROLE_USER` or `ROLE_ADMIN`        |

#### Draw
| Field          | Type           | Constraints  | Description                     |
|----------------|----------------|--------------|---------------------------------|
| id             | BIGINT         | PK           | Unique identifier               |
| date           | TIMESTAMP      | NOT NULL     | Scheduled draw execution time   |
| status         | VARCHAR        | NOT NULL     | OPEN or COMPLETED               |
| winningNumbers | Set\<Integer\> | @ElementCollection | Drawn numbers (populated after execution) |

#### Ticket
| Field        | Type           | Constraints       | Description                      |
|--------------|----------------|-------------------|----------------------------------|
| id           | BIGINT         | PK                | Unique identifier                |
| user_id      | BIGINT         | FK → users.id     | Ticket owner                     |
| draw_id      | BIGINT         | FK → draws.id     | Associated draw                  |
| status       | VARCHAR        | NOT NULL          | PENDING / JACKPOT / HIGH / MEDIUM / NO_PRIZE |
| purchaseTime | TIMESTAMP      | NOT NULL, immutable | Time of purchase               |
| matchCount   | INT            | nullable          | Populated after draw execution   |
| prizeAmount  | DECIMAL(19,2)  | NOT NULL          | 0 until draw is executed         |
| numbers      | Set\<Integer\> | @ElementCollection | Selected numbers               |

### 2.3 Enumerations

```
DrawStatus
├── OPEN        - Draw is accepting ticket purchases
└── COMPLETED   - Draw has been executed, prizes distributed

TicketStatus
├── PENDING     - Draw not yet executed
├── JACKPOT     - 5 matches  → 100,000.00
├── HIGH        - 4 matches  →   5,000.00
├── MEDIUM      - 3 matches  →     250.00
└── NO_PRIZE    - ≤2 matches →       0.00
```

### 2.4 Exception Hierarchy

```
RuntimeException
└── BusinessException
    ├── BadRequestException   → HTTP 400
    ├── NotFoundException     → HTTP 404
    └── ConflictException     → HTTP 409
```

`GlobalExceptionHandler` also maps framework errors to consistent JSON responses:

| Exception | HTTP | Message returned to client |
|-----------|------|----------------------------|
| `MethodArgumentNotValidException` | 400 | Field-level validation messages |
| `HttpMessageNotReadableException` | 400 | `Request body is missing or malformed` |
| `AuthenticationException` (wrong credentials) | 401 | `Invalid username or password` |
| `AccessDeniedException` (missing role) | 403 | `You do not have permission to perform this action` |
| Any other exception | 500 | `An unexpected error occurred` (details are logged server-side only) |

---

## 3. Draw Lifecycle & Prize Distribution

### 3.1 Draw Lifecycle

```
                        ┌───────────────────┐
                        │  POST /draws/start│
                        └─────────┬─────────┘
                                  │
                                  ▼
                        ┌───────────────────┐
                        │  Draw created     │
                        │  status = OPEN    │
                        │  date = now +7d   │
                        └─────────┬─────────┘
                                  │
                    ┌─────────────┴───────────────┐
                    │                             │
          Users buy tickets              Scheduler runs every 60s
          (POST /tickets/buy)            Checks: now > draw.date?
                    │                             │
                    │                             │ YES
                    │                    ┌────────▼────────────┐
                    │                    │  POST /draws/execute│
                    │                    │  (also manual)      │
                    │                    └────────┬────────────┘
                    │                             │
                    │                    ┌────────▼────────────────────┐
                    │                    │  DrawExecutionService        │
                    │                    │  (runs @Async)               │
                    │                    │                              │
                    │                    │  1. Generate winning numbers │
                    │                    │  2. Save to Draw entity      │
                    │                    │  3. Mark Draw = COMPLETED    │
                    │                    │  4. Distribute prizes        │
                    │                    │  5. Create next Draw (OPEN)  │
                    │                    └────────┬────────────────────┘
                    │                             │
                    └─────────────────────────────┘
                                  │
                        ┌─────────▼─────────┐
                        │  Draw COMPLETED   │
                        │  New Draw OPEN    │
                        └───────────────────┘
```

### 3.2 Prize Distribution Strategy

When a draw is executed, `PrizeDistributionService` processes every ticket in that draw:

```
For each Ticket:
  matchCount = |ticket.numbers ∩ winningNumbers|

  matchCount == 5  →  JACKPOT  → prize = 100,000.00
  matchCount == 4  →  HIGH     → prize =   5,000.00
  matchCount == 3  →  MEDIUM   → prize =     250.00
  matchCount <= 2  →  NO_PRIZE → prize =       0.00

  if prize > 0:
      user.balance += prize
      save(user)

  ticket.matchCount  = matchCount
  ticket.status      = category.status
  ticket.prizeAmount = category.prize

saveAll(tickets)
```

**Key design decisions:**
- Prize amounts and number counts are fully configurable via `application.properties` — no hardcoded values in logic.
- Prize category map is built once at startup via `@PostConstruct`, not recalculated per ticket.
- The entire distribution runs inside a single `@Transactional` method — if any step fails, all balance updates and ticket updates roll back atomically.
- Draw execution runs `@Async` so it never blocks the HTTP response.

### 3.3 Lottery Rules

| Rule                | Value                      |
|---------------------|----------------------------|
| Numbers to pick     | 5                          |
| Valid range         | 1 – 49                     |
| Numbers must be     | Unique                     |
| Ticket price        | 10.00                      |
| Initial user balance| 100.00 (10 free tickets)   |
| Draw frequency      | Every 7 days               |
| Scheduler poll rate | Every 60 seconds           |
| Random generator    | `java.security.SecureRandom` |

---

## 4. API Specification

**Base URL:** `http://localhost:8080/api/v1`

**Authentication:** All endpoints except `/auth/**` require:
```
Authorization: Bearer <jwt_token>
```

**Error Response Format (all errors):**
```json
{
  "timestamp": "2026-03-04T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Descriptive error message"
}
```

---

### 4.1 Auth Endpoints

#### POST `/auth/register`
Register a new user. Returns a JWT token immediately.

**Request:**
```json
{
  "username": "john_doe",
  "password": "secret123"
}
```

**Validation:**
- `username`: 3–20 characters, not blank
- `password`: minimum 6 characters, not blank

**Response `201 Created`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 409 Conflict | Username already exists |
| 400 Bad Request | Validation failure |

---

#### POST `/auth/login`
Authenticate with existing credentials.

**Request:**
```json
{
  "username": "john_doe",
  "password": "secret123"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 401 Unauthorized | Wrong password |
| 404 Not Found | User does not exist |

---

### 4.2 Ticket Endpoints

#### POST `/tickets/buy`
Purchase a lottery ticket for the currently open draw.
The authenticated user is identified from the JWT — no userId needed in the request body.

**Request:**
```json
{
  "selectedNumbers": [7, 14, 21, 35, 49]
}
```

**Validation:**
- Exactly 5 numbers required
- All numbers must be unique
- Each number must be in range [1, 49]
- User must have sufficient balance (≥ 10.00)
- An OPEN draw must exist

**Response `201 Created`:**
```json
"New ticket created!"
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 Bad Request | Wrong number count, duplicates, out of range, or insufficient balance |
| 404 Not Found | No active draw exists |
| 401 Unauthorized | Missing or invalid JWT |

---

#### GET `/tickets/my-results`
Returns all tickets purchased by the authenticated user, ordered by purchase time descending.

**Response `200 OK`:**
```json
[
  {
    "ticketId": 1,
    "drawId": 1,
    "purchaseTime": "2026-03-01T10:30:00",
    "selectedNumbers": [7, 14, 21, 35, 49],
    "drawStatus": "COMPLETED",
    "winningNumbers": [7, 14, 21, 3, 9],
    "matchCount": 3,
    "prize": 250.00,
    "ticketStatus": "MEDIUM"
  },
  {
    "ticketId": 2,
    "drawId": 2,
    "purchaseTime": "2026-03-04T09:00:00",
    "selectedNumbers": [1, 2, 3, 4, 5],
    "drawStatus": "OPEN",
    "winningNumbers": null,
    "matchCount": null,
    "prize": 0.00,
    "ticketStatus": "PENDING"
  }
]
```

> `winningNumbers` and `matchCount` are `null` while the draw is still OPEN.

---

### 4.3 Draw Endpoints

#### POST `/draws/start`
Create a new draw. Fails if an OPEN draw already exists.

> **Requires `ROLE_ADMIN`.** Returns `403 Forbidden` for regular users.

**Request:** _(no body)_

**Response `201 Created`:**
```json
{
  "id": 1,
  "date": "2026-03-11T12:00:00",
  "status": "OPEN",
  "winningNumbers": null
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 409 Conflict | An OPEN draw already exists |

---

#### POST `/draws/execute`
Manually trigger execution of the current open draw.
The draw also executes automatically when its scheduled date is reached (checked every 60 seconds).

> **Requires `ROLE_ADMIN`.** Returns `403 Forbidden` for regular users.

**Request:** _(no body)_

**Response `200 OK`:**
```
"Draw execution started. Results will be processed asynchronously."
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 404 Not Found | No OPEN draw exists |

---

#### GET `/draws/history`
Returns all completed draws with winning numbers and winner counts per prize category.

**Response `200 OK`:**
```json
[
  {
    "drawId": 1,
    "date": "2026-03-04T12:00:00",
    "winningNumbers": [3, 7, 14, 21, 49],
    "winnersPerCategory": {
      "JACKPOT": 0,
      "HIGH": 1,
      "MEDIUM": 3
    }
  }
]
```

> `winnersPerCategory` keys are `TicketStatus` names (`JACKPOT`, `HIGH`, `MEDIUM`). Only winning tickets (matchCount ≥ 3) are included. Categories with zero winners are omitted from the map.

---

### 4.4 Endpoint Summary

| Method | Endpoint                  | Auth Required      | Description                    |
|--------|---------------------------|:------------------:|--------------------------------|
| POST   | `/auth/register`          | No                 | Register new user              |
| POST   | `/auth/login`             | No                 | Login, get JWT token           |
| POST   | `/tickets/buy`            | Yes                | Purchase a ticket              |
| GET    | `/tickets/my-results`     | Yes                | Get own tickets with results   |
| POST   | `/draws/start`            | Yes (ADMIN only)   | Start a new draw               |
| POST   | `/draws/execute`          | Yes (ADMIN only)   | Manually execute current draw  |
| GET    | `/draws/history`          | Yes                | Get completed draw history     |

---

## 5. Setup Instructions

### 5.1 Prerequisites

| Tool   | Version  |
|--------|----------|
| Docker | 24+      |
| Docker Compose | v2+ |
| Java   | 17+ (only if running locally without Docker) |
| Maven  | 3.9+ (only if running locally without Docker) |

---

### 5.2 Run with Docker (Recommended)

```bash
# 1. Clone the repository
git clone https://github.com/cakmakbm/LottoFun.git
cd LottoFun

# 2. Build and start all services (app + PostgreSQL)
docker compose up --build

# 3. Application is available at:
#    http://localhost:8080
```

To stop:
```bash
docker compose down

# To also remove the database volume (clean slate):
docker compose down -v
```

To rebuild after code changes:
```bash
docker compose up --build --force-recreate
```

---

### 5.3 Run Locally (without Docker)

Requires PostgreSQL running locally.

```bash
# 1. Start PostgreSQL (or use existing instance)
#    Create a database named "lottofun"

# 2. Configure application.properties
#    src/main/resources/application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/lottofun
spring.datasource.username=your_user
spring.datasource.password=your_password

# 3. Build the project
./mvnw clean package -DskipTests

# 4. Run the application
java -jar target/LottoFun-0.0.1-SNAPSHOT.jar
```

---

### 5.4 Environment Variables (Docker Compose)

All configurable values are passed as environment variables:

| Variable                        | Default          | Description               |
|---------------------------------|------------------|---------------------------|
| `SPRING_DATASOURCE_URL`         | _(required)_     | PostgreSQL JDBC URL       |
| `SPRING_DATASOURCE_USERNAME`    | _(required)_     | DB username               |
| `SPRING_DATASOURCE_PASSWORD`    | _(required)_     | DB password               |
| `JWT_SECRET`                    | _(required)_     | Base64-encoded HMAC key, at least 256 bits (`openssl rand -base64 32`) |
| `ADMIN_PASSWORD`                | _(empty)_        | Admin account password; admin is not created when empty |
| `POSTGRES_PASSWORD`             | _(required)_     | Database password, read from `.env` by Docker Compose |
| `JWT_EXPIRATION`                | `86400000`       | Token TTL in milliseconds |
| `LOTTERY_RULE_NUMBER_COUNT`     | `5`              | Numbers per ticket        |
| `LOTTERY_RULE_MIN_NUMBER`       | `1`              | Minimum selectable number |
| `LOTTERY_RULE_MAX_NUMBER`       | `49`             | Maximum selectable number |
| `LOTTERY_RULE_TICKET_PRICE`     | `10.00`          | Price per ticket          |
| `LOTTO_INITIAL_BALANCE`         | `100.00`         | Balance given at register |
| `LOTTO_PRIZE_JACKPOT`           | `100000.00`      | 5-match prize             |
| `LOTTO_PRIZE_HIGH`              | `5000.00`        | 4-match prize             |
| `LOTTO_PRIZE_MEDIUM`            | `250.00`         | 3-match prize             |

---

### 5.5 Quick Start — Example Flow

```bash
# 1. Login as admin (auto-created at startup)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<your ADMIN_PASSWORD>"}'
# → {"token":"eyJ..."}

ADMIN_TOKEN="eyJ..."

# 2. Start a draw (admin only)
curl -X POST http://localhost:8080/api/v1/draws/start \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 3. Register a regular user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"pass123"}'
# → {"token":"eyJ..."}

USER_TOKEN="eyJ..."

# 4. Buy a ticket
curl -X POST http://localhost:8080/api/v1/tickets/buy \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedNumbers":[7,14,21,35,49]}'

# 5. Execute the draw manually (admin only)
curl -X POST http://localhost:8080/api/v1/draws/execute \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 6. Check your results
curl http://localhost:8080/api/v1/tickets/my-results \
  -H "Authorization: Bearer $USER_TOKEN"
```

---

## 6. Testing with Postman

The `postman/` directory contains the API collection. You need to create a local environment manually before running requests.

### 6.1 Import the Collection

1. Open Postman
2. Click **Import** → select `postman/LottoFun API.postman_collection.json`

### 6.2 Create a Local Environment

1. Click **Environments** in the left sidebar → **+** (New Environment)
2. Name it `LottoFun Local`
3. Add the following variables:

| Variable      | Initial Value                  |
|---------------|--------------------------------|
| `base_url`    | `http://localhost:8080/api/v1` |
| `token`       | _(leave empty)_                |
| `admin_token` | _(leave empty)_                |

4. Click **Save**
5. Select **LottoFun Local** from the environment dropdown (top-right)

### 6.3 Suggested Test Flow

Tokens are saved automatically via collection scripts — no manual copy-paste needed.

```
1. POST /auth/login          (admin credentials)  → admin_token saved automatically
2. POST /draws/start                              → creates an OPEN draw
3. POST /auth/register       (any username)       → token saved automatically
4. POST /tickets/buy                              → purchase a ticket
5. POST /draws/execute                            → triggers prize distribution
6. GET  /tickets/my-results                       → verify ticket status & prize
7. GET  /draws/history                            → verify draw results
```

---

## 7. Testing Approach

### 7.1 Test Strategy

The project uses **unit testing** with JUnit 5, Mockito, and AssertJ. Unit tests are isolated — no Spring context is loaded, no database is required. One integration test runs against an in-memory H2 database to verify balance updates under concurrent requests.

```
src/test/java/com/bmc/lottofun/service/
├── TicketValidatorTest.java       (6 tests  — pure logic, zero mocks)
├── UserServiceTest.java           (4 tests  — atomic financial operations)
├── TicketServiceTest.java         (4 tests  — core purchase flow)
├── PrizeDistributionServiceTest.java (6 tests — prize categories)
├── DrawServiceTest.java           (4 tests  — draw management)
├── AuthServiceTest.java           (4 tests  — register & login)
└── BalanceConcurrencyIntegrationTest.java (1 test — 20 concurrent purchases, H2)

src/test/java/com/bmc/lottofun/exception/
└── GlobalExceptionHandlerTest.java (3 tests — 500 / 401 / 403 responses)
```

**Total: 31 unit tests + 1 concurrency integration test**

### 7.2 What Each Test Suite Covers

| Test Class | Focuses On |
|------------|-----------|
| `TicketValidatorTest` | Boundary values, count check, uniqueness, range validation |
| `UserServiceTest` | Atomic deduct (success / insufficient), non-positive amount rejected, atomic add |
| `GlobalExceptionHandlerTest` | Internal error messages are hidden, wrong credentials → 401, missing role → 403 |
| `BalanceConcurrencyIntegrationTest` | 20 simultaneous purchases on a 5-ticket balance: exactly 5 succeed, balance ends at 0 |
| `TicketServiceTest` | Happy path, no active draw, validation abort, single deduction guarantee |
| `PrizeDistributionServiceTest` | All 4 prize tiers, multi-ticket distribution, empty draw |
| `DrawServiceTest` | Create draw, conflict on duplicate, retrieve active draw, not found |
| `AuthServiceTest` | New user registration, duplicate username, valid login, bad credentials |

### 7.3 Key Testing Techniques Used

| Technique | Used In | Purpose |
|-----------|---------|---------|
| `@ExtendWith(MockitoExtension.class)` | All service tests | Lightweight Mockito injection |
| `ReflectionTestUtils.setField()` | TicketService, PrizeDistribution, Auth tests | Inject `@Value` fields without Spring context |
| `ArgumentCaptor` | TicketService, DrawService, AuthService tests | Assert what was actually saved to the repository |
| `verify(mock, times(1))` | TicketServiceTest | Guard against double balance deduction |
| `verify(mock, never())` | Multiple tests | Assert no side effects on failure paths |
| `assertThatThrownBy()` | All exception tests | Assert exception type and message |
| `assertThatCode().doesNotThrowAnyException()` | TicketValidatorTest | Assert happy paths |
| `initPrizeCategories()` manually | PrizeDistributionTest | Replicate `@PostConstruct` in unit test |

### 7.4 Running the Tests

```bash
# Option 1: Local (no Docker required)
./mvnw test

# Run a specific test class
./mvnw test -Dtest=TicketServiceTest

# Run with detailed output
./mvnw test -Dsurefire.useFile=false

# Option 2: Docker — tests run automatically during image build
# The Dockerfile uses `mvn verify`, so tests execute on every build.
# If any test fails, the build stops and no image is produced.
docker compose build
```

### 7.5 Test Coverage by Component

```
                        Covered by unit tests?
                              │
  TicketValidator ────────────┤ YES — all validation branches
  UserService ────────────────┤ YES — all balance operations
  TicketService ──────────────┤ YES — all purchase branches
  PrizeDistributionService ───┤ YES — all 4 prize categories
  DrawService ────────────────┤ YES — start, conflict, get, not found
  AuthService ────────────────┤ YES — register, duplicate, login, bad creds
  DrawExecutionService ───────┤ PARTIAL — async orchestration; integration test recommended
  NumberGeneratorService ─────┤ NOT COVERED — SecureRandom, statistical; property-based test recommended
  JwtService ─────────────────┤ NOT COVERED — crypto; tested implicitly via auth flow
  AuthHelper ─────────────────┤ NOT COVERED — thin wrapper; tested via TicketService mock
```
