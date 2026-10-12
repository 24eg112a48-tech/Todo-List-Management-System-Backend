# To-Do List Management System — Backend

REST API for the To-Do List Management System, built with **Java 21, Spring Boot 3, Spring Data JPA, and an embedded H2 file database**. Single-user local application — no login, no external services.

Companion repo: **Todo-List-Management-System-Frontend** (standalone HTML/CSS/vanilla-JS dashboard that consumes this API).

## Technology Stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 (Spring Web, Spring Data JPA, Bean Validation) |
| Database | Embedded H2 in **file mode** (`jdbc:h2:file:./data/tododb`), Hibernate `ddl-auto=update` |
| Tests | JUnit 5, Mockito, MockMvc (via `spring-boot-starter-test`) |
| Build | Maven Wrapper (`./mvnw`) |

## Prerequisites

- Java 21+ (`java -version`)
- Nothing else: no MySQL/PostgreSQL, no Docker, no accounts.

## Architecture

```
src/main/java/com/example/todo/
  TodoApplication.java
  entity/      Task.java               (JPA entity: id, title, description,
                                        priority, status, dueDate,
                                        createdAt, updatedAt)
  enums/       Priority.java           (LOW, MEDIUM, HIGH)
               TaskStatus.java         (PENDING, COMPLETED)
  repository/  TaskRepository.java      (Spring Data JPA + overdue query)
  service/     TaskService.java        (all business logic: CRUD, complete/
                                        reopen, search/filter/sort, stats)
  controller/  TaskController.java      (thin REST layer, /api/v1/tasks)
  dto/         TaskRequest.java        (validated input)
               TaskResponse.java       (output + computed overdue flag)
               StatsResponse.java      (total, pending, completed,
                                        overdue, completionPercentage)
  exception/   ResourceNotFoundException.java
               GlobalExceptionHandler.java  (centralized JSON error handling)
  config/      CorsConfig.java         (allows the standalone frontend
                                        origin during local development)
```

Business logic lives in `TaskService`; controllers only map HTTP ↔ DTOs.

### Authentication

Omitted by design: this is a **single-user local application**. If multi-user auth is ever added, the intended approach is Spring Security with BCrypt password hashing and JWT — no external providers.

## H2 Configuration

`src/main/resources/application.properties`:

```properties
server.port=8080
spring.datasource.url=jdbc:h2:file:./data/tododb;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

- Tables are created automatically by Hibernate on startup.
- Data persists in `./data/tododb.mv.db` **across restarts** (verified). `DB_CLOSE_ON_EXIT=FALSE` prevents corruption on shutdown.
- H2 console (dev only): http://localhost:8080/h2-console — JDBC URL `jdbc:h2:file:./data/tododb`, user `sa`, blank password.
- Tests use a separate in-memory DB (`application-test.properties`, `create-drop`), so they never touch `./data`.
- No credentials/secrets are committed (default blank `sa` password, local dev only).

## How to Run the Backend

```bash
cd Todo-List-Management-System-Backend
./mvnw spring-boot:run
```

The API is then available at **http://localhost:8080**. (On Windows use `mvnw.cmd`. If port 8080 is taken: `lsof -ti:8080 | xargs kill -9`, or override with `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`.)

Packaged run:

```bash
./mvnw clean package
java -jar target/todo-list-1.0.0.jar
```

## Production Configuration

All production knobs are environment variables (no code changes, no secrets in git):

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port (hosts like Render inject their own) |
| `TODO_DB_PATH` | `./data/tododb` | H2 file path **without** `.mv.db` suffix — must point at persistent writable storage |
| `TODO_CORS_PATTERNS` | `*` | Comma-separated allowed-origin patterns, e.g. `https://todo-frontend.vercel.app` |

```bash
PORT=8081 TODO_DB_PATH=/var/data/tododb TODO_CORS_PATTERNS=https://my-frontend.vercel.app \
  java -jar target/todo-list-1.0.0.jar
```

Liveness probe for hosting providers: `GET /api/v1/health` → `{"status":"UP"}`.

### Docker / Render

```bash
docker build -t todo-backend .
docker run -p 8080:8080 -e TODO_CORS_PATTERNS=https://my-frontend.vercel.app todo-backend
```

`render.yaml` is a Render blueprint (Docker runtime, health check wired). **Persistence warning:** Render's free tier has an *ephemeral* filesystem — the file-based H2 database is wiped on every restart/redeploy, and free services cannot attach persistent disks (paid plans only). Until a paid disk (`TODO_DB_PATH=/var/data/tododb`) or a managed database is approved, do **not** treat a free-tier deploy as persistent. Switching to managed Postgres would change the stack and needs explicit approval — it has not been done.

## API Documentation

Base URL: `http://localhost:8080/api/v1/tasks`

| Method | Endpoint | Success | Description |
|---|---|---|---|
| `POST` | `/api/v1/tasks` | `201` | Create a task |
| `GET` | `/api/v1/tasks` | `200` | List with search/filter/sort |
| `GET` | `/api/v1/tasks/stats` | `200` | Dashboard statistics |
| `GET` | `/api/v1/tasks/{id}` | `200` / `404` | Get one task |
| `PUT` | `/api/v1/tasks/{id}` | `200` / `404` | Update a task |
| `DELETE` | `/api/v1/tasks/{id}` | `204` / `404` | Delete a task |
| `PATCH` | `/api/v1/tasks/{id}/complete` | `200` / `404` | Mark completed |
| `PATCH` | `/api/v1/tasks/{id}/reopen` | `200` / `404` | Reopen (back to PENDING) |

List query parameters: `search` (matches title/description, case-insensitive), `status` (`PENDING`/`COMPLETED`), `priority` (`LOW`/`MEDIUM`/`HIGH`), `overdue=true`, `sortBy` (`createdAt`/`updatedAt`/`dueDate`/`priority`/`title`/`status`), `direction` (`asc`/`desc`).

Invalid input (blank title, bad enum, malformed JSON) → `400` with `{timestamp, status, error, message, fieldErrors?}`. Missing IDs → `404`.

### Example requests and responses

```bash
# Create
curl -X POST http://localhost:8080/api/v1/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Finish report","description":"Q3 summary","priority":"HIGH","dueDate":"2026-10-20"}'
# → 201 {"id":1,"title":"Finish report",...,"status":"PENDING","createdAt":"...","updatedAt":"...","overdue":false}

# List: pending high-priority, earliest due first
curl 'http://localhost:8080/api/v1/tasks?status=PENDING&priority=HIGH&sortBy=dueDate&direction=asc'

# Overdue only
curl 'http://localhost:8080/api/v1/tasks?overdue=true'

# Statistics
curl http://localhost:8080/api/v1/tasks/stats
# → {"total":2,"pending":1,"completed":1,"overdue":1,"completionPercentage":50.0}

# Complete / reopen / delete
curl -X PATCH http://localhost:8080/api/v1/tasks/1/complete
curl -X PATCH http://localhost:8080/api/v1/tasks/1/reopen
curl -X DELETE http://localhost:8080/api/v1/tasks/1   # → 204, then GET → 404
```

## How to Run Tests

```bash
cd Todo-List-Management-System-Backend
./mvnw clean test
```

Covers: creation, list, get-by-id, update, delete, complete/reopen, invalid input (blank title, bad enum), missing IDs (404), search, status/priority/overdue filtering, stats math (Mockito unit test with mocked repository), and in-memory DB persistence.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `java -version` < 21 | Install JDK 21, set `JAVA_HOME` |
| Port 8080 in use | `lsof -ti:8080 \| xargs kill -9`, or `--server.port=8081` override |
| H2 console login fails | JDBC URL must be exactly `jdbc:h2:file:./data/tododb`, user `sa`, blank password |
| `data/` locked | Stop the app first; only one instance may use the file DB |
| Frontend gets CORS errors | Backend `CorsConfig` already allows the standalone frontend; ensure you didn't change/remove it |
| `mvnw: Permission denied` | `chmod +x mvnw` |
