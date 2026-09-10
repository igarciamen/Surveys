# Public surveys: a survey platform built on microservices. 

A survey-building platform built as a system of **independent
microservices**. Six backend services in Spring Boot 4 (five of them
accessible through a custom **gateway**, four with their own PostgreSQL
database) and an Angular frontend. It includes JWT authentication with
roles, survey creation with different question types, access control (open
/ password-protected / restricted to registered users), response submission
and aggregation, statistics, and email invitations with scheduled automatic
survey closing.

---
## Demo

https://github.com/user-attachments/assets/16aa058d-f6b3-414d-9aab-1c85d778fbd2

## Architecture

Each microservice is autonomous: it has its own port, its own database (if
it needs one), and its own lifecycle. They don't share tables. All traffic
enters through a single **gateway** (Spring Cloud Gateway, WebFlux reactive
flavor) that routes each `/api/<area>/**` prefix to the corresponding
microservice without rewriting the path, forwarding the `Authorization`
header as-is. When one service needs data from another (for example,
`surveys` needs the owner's name, or `responses` needs to re-read the
question schema), it makes an **HTTP call forwarding the user's JWT token**,
rather than accessing the other service's database directly.

| Service         | Port | Database                | Responsibility                                          |
|------------------|:------:|-------------------------|---------------------------------------------------------|
| `gateway`        | 8080   | -                        | Single entry point, static routing                      |
| `users`          | 8081   | `usersurveyDB`           | Registration, login, JWT issuance, roles                |
| `surveys`        | 8082   | `surveysDB`              | Surveys, questions, options, publish/close               |
| `responses`      | 8083   | `responsesDB`            | Response submission, validation, aggregated results      |
| `statistics`     | 8084   | - (consumes `responses`) | Statistics and time series for a survey                  |
| `invitations`    | 8085   | `invitationsSurveyDB`    | Email invitations, access tokens                         |
| `notifications`  | 8086   | - (SMTP)                 | Email sending                                             |
| *frontend*       | 4200   | -                        | Angular application (outside Docker by default)          |

A **single PostgreSQL instance** hosts the four persistent databases
(created by `init-databases.sql` on first startup).

---

## Tech Stack

**Backend**
- Java 21
- Spring Boot 4 (Web MVC, Data JPA, Validation, Security)
- Spring Cloud Gateway (WebFlux) for the `gateway`
- Spring Security as an **OAuth2 Resource Server** (JWT validation, HMAC-SHA256)
- `io.jsonwebtoken` (jjwt) for token issuance in `users`
- PostgreSQL 16
- springdoc-openapi (Swagger UI)
- Apache HttpClient 5 (`RestTemplate` between services, with an interceptor that forwards the JWT)
- Spring Scheduling (`SurveyAutoCloseTask`) for automatic survey closing
- Spring Mail (SMTP) in `notifications`

**Frontend**
- Angular (standalone components)
- HTTP interceptor to attach the JWT (`auth-interceptor`) and role-based `AuthGuard`
- RxJS

**Infrastructure**
- Docker Compose (Postgres + 6 microservices; the frontend runs separately with `ng serve`)

---

## Repository Structure

```
eusurvey/
├── docker-compose.yml         # Postgres + 6 microservices (gateway included)
├── init-databases.sql         # creates the 4 persistent databases
│
├── gateway/                   # Spring Cloud Gateway, no database
│   ├── pom.xml
│   └── src/main/java/com/igarciamen/gateway/...
├── users/                     # each microservice: pom.xml + src/
│   └── src/main/java/com/igarciamen/users/...
├── surveys/
│   └── src/main/java/com/igarciamen/surveys/...
├── responses/
│   └── src/main/java/com/igarciamen/responses/...
├── statistics/
│   └── src/main/java/com/igarciamen/statistics/...
├── invitations/
│   └── src/main/java/com/igarciamen/invitations/...
├── notifications/
│   └── src/main/java/com/igarciamen/notifications/...
│
└── frontend/                  # Angular (runs with ng serve)
    └── src/app/...
```

Base package for each service: `com.igarciamen.<service>`, following the
usual layered organization: `model`, `repository`, `payloads` (request/
response DTOs), `service`, `controller`, `config`, and `client` (in
services that call others: `surveys`→`users`, `responses`→`surveys`,
`statistics`→`responses`, `invitations`→`surveys`/`notifications`).

---

## The Microservices

### gateway (8080)
The single entry point. Static route table (`GatewayConfig`): each
`/api/<area>/**` prefix is forwarded unchanged to the owning microservice
(`/api/auth/**` and `/api/user(s)/**` → `users`; `/api/surveys/**` →
`surveys`; `/api/responses/**` → `responses`; `/api/statistics/**` →
`statistics`; `/api/invitations/**` → `invitations`). It does not validate
the JWT itself; it simply forwards the `Authorization` header, and each
microservice validates it as a *Resource Server*.

### users (8081)
Manages identity. Registration (`/api/auth/signup`) and login
(`/api/auth/login`), which returns a **JWT signed with HMAC-SHA256**
carrying the `userId` and `roles` claims. It also exposes the authenticated
user's data (`/api/user/me`) and a user by id (`/api/user/{id}`, used by
other services to enrich their responses). Available roles: `ROLE_USER`,
`ROLE_DESIGNER`, `ROLE_ADMIN`. Seeds an admin user on startup
(`DataLoader`, configurable via `app.admin.username` / `app.admin.password`).

### surveys (8082)
CRUD for surveys along with their question/option tree
(`Survey 1—N Question 1—N QuestionOption`). Lifecycle controlled by
`SurveyStatus` (`DRAFT → PUBLISHED → CLOSED`, one-way transition): editing
is only allowed in `DRAFT`, and publishing requires at least one question.
Access is controlled via `SurveyAccess` (`OPEN` / `PASSWORD` /
`RESTRICTED`); the password is stored as plain text and **never** serialized
back to the client (`@JsonProperty(WRITE_ONLY)`). Exposes an internal
management endpoint (`/{id}/manage`) that requires being the owner or an
admin, used by `invitations`. Enriches each survey with owner data by
calling `users`. Includes a scheduled task (`SurveyAutoCloseTask` +
`SchedulingConfig`) that automatically closes surveys whose `closesAt` has
already passed.

### responses (8083)
Receives a response submission (`POST /api/responses/survey/{id}`). Before
accepting it: it re-fetches the published schema from `surveys` (it doesn't
trust what the client sends), applies the access-mode rules (`RESTRICTED`
requires login; `PASSWORD` requires login + a correct password verified
against `surveys`; `OPEN` allows anonymous submissions), prevents a second
submission from the same registered user for the same survey, and validates
each answer against the question type (required fields, valid options,
etc.). It also computes aggregated results per question (`/results`), the
submission timestamps for each entry (`/timestamps`, used by `statistics`),
and allows exporting responses as **CSV** and **Excel** (owner or admin).

### statistics (8084, no database)
A read-only, aggregation-only service: it asks `responses` for a survey's
results and timestamps and builds a view with the total number of
responses, the first/last response, a daily time series, and a highlight
per question (most-chosen option, average, yes/no, or number of free-text
responses).

### invitations (8085)
Invites people to a survey by email. When creating invitations, it verifies
that the requester is the owner/admin of the survey (`GET /{id}/manage` on
`surveys`), cleans and deduplicates the email addresses, generates a unique
token per invitation, and asks `notifications` to send the email with the
link (`{frontend}/surveys/{id}/answer?invite={token}`); if the email fails,
the invitation is still created (best-effort). The recipient can validate
their token (`GET /token/{token}`) and mark it as answered
(`POST /token/{token}/accept`).

### notifications (8086, no database)
Internal email-sending service over SMTP. Exposes
`POST /api/notifications/invitation`, intended to be called only by
`invitations`.

---

## Data Model

Main entities:

- **User** ⟷ **Role** (`ManyToMany`), `Role.name` is the **ERole** enum.
- **Survey** *1—N* **Question** *1—N* **QuestionOption** (real FKs within `surveysDB`).
- **SurveyResponse** *1—N* **Answer** (real FKs within `responsesDB`); each
  `Answer` stores either a simple value (`textValue`) or a list of chosen
  option ids (`selectedOptionIds`), depending on the question type.
- **Invitation** (a single table within `invitationsSurveyDB`, with no children).

Since each service has its own database, **there are no foreign keys
across services**: cross-references (for example `Survey.ownerUserId`,
`SurveyResponse.surveyId`, `Answer.questionId`, `Invitation.surveyId`) are
logical identifiers that are resolved via HTTP or validated at write time.

---

## Security (JWT + Roles)

- `users` issues the JWT at login, signed with a shared HMAC secret key
  (`jwt.secret`), carrying the `userId` and `roles` claims.
- The other services act as **Resource Servers**: they validate the token
  using the same key (`NimbusJwtDecoder`, HmacSHA256). Roles are read from
  the `roles` claim without any added prefix.
- The `gateway` does not validate the JWT: it only routes and forwards the
  `Authorization` header unmodified.
- The frontend stores the token and attaches it to every request via an
  **interceptor** (`Authorization: Bearer ...`). Between services, the
  token is **forwarded** by a `RestTemplate` interceptor
  (`RestTemplateConfig`) that copies the `Authorization` header from the
  incoming request to the outgoing one.
- Typical rules: the catalog of published surveys and answering `OPEN`
  surveys are public; creating/editing/publishing/closing/deleting a survey,
  viewing results/statistics, and sending invitations require being the
  owner or `ROLE_ADMIN`; Swagger and `/api/auth/**` are public.

---

## Angular Frontend

A standalone application with routing protected by `AuthGuard` and by role
(`ROLE_DESIGNER` / `ROLE_ADMIN` depending on the route). Main routes:

```
/login  /signup           authentication

/surveys                  public catalog of published surveys
/surveys/:id               survey detail
/surveys/:id/answer         answer a survey (public; RESTRICTED requires login)

/surveys/new                create a survey (DESIGNER/ADMIN)
/surveys/manage              manage my surveys (DESIGNER/ADMIN)
/surveys/:id/edit            edit a survey in DRAFT (DESIGNER/ADMIN)
/surveys/:id/results          aggregated results (owner/ADMIN)
/surveys/:id/statistics        statistics (owner/ADMIN)
/surveys/:id/invitations        invitations (owner/ADMIN)
```

---

## Running with Docker

Requirements: Docker and Docker Compose.

```bash
# From the root folder (where docker-compose.yml is located)
docker compose up --build
```

The first run builds the six images and creates the databases. Once
PostgreSQL is *healthy*, `users`, `surveys`, `responses`, `statistics`,
`invitations` start up, and finally `gateway` (which depends on all of the
above).


Frontend (outside Docker):

```bash
cd frontend
npm install
ng serve
# http://localhost:4200
```

Relevant environment variables (see `docker-compose.yml` and `_env`):
`MAIL_USERNAME` / `MAIL_PASSWORD` (SMTP for `notifications`),
`FRONTEND_BASE_URL` (used by `invitations` to build the invitation link).

Stopping and persistence:

```bash
docker compose down       # keeps the data (postgres-data volume)
docker compose down -v    # also removes the data (fresh start)
docker compose logs -f surveys   # view logs for a service
```

URLs between containers and the database connection are injected via
**environment variables** in `docker-compose.yml`, so the
`application.properties` files (which use `localhost`) remain untouched
and still work for running from IntelliJ.

---

## Running Locally (IntelliJ)

1. Start a local PostgreSQL instance and create the four databases
   (`usersurveyDB`, `surveysDB`, `responsesDB`, `invitationsSurveyDB`).
2. Adjust credentials in each `application.properties` if they differ.
3. Run each microservice as a Spring Boot application (including `gateway`).
4. `ng serve` for the frontend.

---

## API — Endpoint Summary

All accessible through the gateway at `http://localhost:8080`.

**users**
```
POST /api/auth/signup
POST /api/auth/login
GET  /api/user/me
GET  /api/user/{id}
```

**surveys**
```
GET    /api/surveys?q=                     (public, paginated)
GET    /api/surveys/{id}                   (public, PUBLISHED only)
GET    /api/surveys/mine                   (owner)
GET    /api/surveys/{id}/manage            (owner/ADMIN, any status)
POST   /api/surveys                        (owner)
PUT    /api/surveys/{id}                   (owner/ADMIN, DRAFT only)
PATCH  /api/surveys/{id}/status             (owner/ADMIN; DRAFT→PUBLISHED→CLOSED)
DELETE /api/surveys/{id}                   (owner/ADMIN)
POST   /api/surveys/{id}/verify-password    (public)
```

**responses**
```
POST /api/responses/survey/{surveyId}                  (public or optional, depending on access)
GET  /api/responses/survey/{surveyId}/mine              (authenticated)
GET  /api/responses/survey/{surveyId}/results            (owner/ADMIN)
GET  /api/responses/survey/{surveyId}/timestamps          (owner/ADMIN)
GET  /api/responses/survey/{surveyId}/export/csv           (owner/ADMIN)
GET  /api/responses/survey/{surveyId}/export/xlsx            (owner/ADMIN)
```

**statistics**
```
GET /api/statistics/survey/{surveyId}       (owner/ADMIN)
```

**invitations**
```
POST   /api/invitations/survey/{surveyId}                          (owner/ADMIN)
GET    /api/invitations/survey/{surveyId}                          (owner/ADMIN)
GET    /api/invitations/token/{token}                              (authenticated)
POST   /api/invitations/token/{token}/accept                       (authenticated)
DELETE /api/invitations/{invitationId}                             (owner/ADMIN)
```

**notifications** (internal use)
```
POST /api/notifications/invitation
```
