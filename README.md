# TaskMaster

REST API for tracking tasks inside teams. Users register, join teams, assign work, and collaborate with comments and file attachments.

## Prerequisites

- JDK 25 (the Gradle build uses a Java 25 toolchain)
- Docker, for PostgreSQL 17

`JAVA_HOME` on this machine may point at a JDK 21 install. Leave it as-is. Gradle detects the JDK 25 under `/Library/Java/JavaVirtualMachines` and compiles with that toolchain. If detection fails, point Gradle at JDK 25 for the session:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home
```

## Run

PostgreSQL is defined in `docker-compose.yml` (`taskmaster` / `taskmaster` on port 5432). `bootRun` starts that Compose file for you.

```bash
./gradlew bootRun
```

The wrapper downloads Gradle 9.4.0 on first use. If that download is blocked, `gradle bootRun` uses a local Gradle 9.4 install instead.

Or start the database yourself and run the app the same way:

```bash
docker compose up -d
./gradlew bootRun
```

- API base path: `http://localhost:8080/api`
- OpenAPI UI: `http://localhost:8080/swagger-ui.html`

Set `JWT_SECRET` to a value of at least 32 bytes before using this outside a local machine. Attachments are stored in `./data/attachments` and limited to 10 MB.

## Sample flow

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","password":"password1","displayName":"Ada"}'

curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","password":"password1"}'

curl -s http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"

curl -s -X POST "http://localhost:8080/api/teams/$TEAM_ID/tasks" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Write the spec","description":"API contract","dueDate":"2026-12-01"}'

curl -s "http://localhost:8080/api/tasks?assignee=me&status=OPEN&q=spec&sort=dueDate,asc" \
  -H "Authorization: Bearer $ACCESS_TOKEN"

curl -s -X POST http://localhost:8080/api/auth/logout \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

Registration creates a personal team, so a new account can create tasks immediately. Invite teammates by email after they have registered:

`POST /api/teams/{teamId}/members` with `{ "email": "grace@example.com" }`.

Logout revokes the refresh token. The access token cannot be revoked server-side; clients should discard it. It expires after 15 minutes.

## API

| Area | Endpoints |
| --- | --- |
| Auth | `POST /api/auth/register`, `/login`, `/refresh`, `/logout` |
| Profile | `GET /api/users/me`, `PATCH /api/users/me` |
| Teams | `POST /api/teams`, `GET /api/teams`, `GET /api/teams/{teamId}`, `POST /api/teams/{teamId}/members`, `DELETE /api/teams/{teamId}/members/{userId}` |
| Tasks | `POST /api/teams/{teamId}/tasks`, `GET /api/tasks`, `GET /api/tasks/{taskId}`, `PATCH /api/tasks/{taskId}`, `POST /api/tasks/{taskId}/complete`, `DELETE /api/tasks/{taskId}` |
| Comments | `GET/POST /api/tasks/{taskId}/comments`, `DELETE /api/tasks/{taskId}/comments/{commentId}` |
| Attachments | `GET/POST /api/tasks/{taskId}/attachments`, `GET/DELETE /api/tasks/{taskId}/attachments/{attachmentId}` |

`GET /api/tasks` accepts `status` (`OPEN`, `IN_PROGRESS`, `COMPLETED`), `teamId`, `assignee` (`me` or a user id), `q` (title or description), and `sort` (`dueDate`, `createdAt`, or `title`, with `,asc` or `,desc`).

Errors use `application/problem+json`.

## Tests

```bash
./gradlew test
```

Integration tests start PostgreSQL with Testcontainers, so Docker needs to be running.
