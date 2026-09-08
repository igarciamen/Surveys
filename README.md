# EUSurvey — Plataforma de encuestas con microservicios

Plataforma de encuestas (survey builder) construida como un sistema de
**microservicios independientes**. Seis servicios backend en Spring Boot 4
(cinco de ellos accesibles a través de un **gateway** propio, cuatro con su
propia base de datos PostgreSQL) y un frontend Angular. Incluye
autenticación con JWT y roles, creación de encuestas con distintos tipos de
pregunta, control de acceso (abierta / con contraseña / restringida a
usuarios registrados), envío y agregación de respuestas, estadísticas, e
invitaciones por email con cierre automático programado de encuestas.

---
## Demo


https://github.com/user-attachments/assets/16aa058d-f6b3-414d-9aab-1c85d778fbd2


## Índice

- [Arquitectura](#arquitectura)
- [Stack tecnológico](#stack-tecnológico)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Los microservicios](#los-microservicios)
- [Modelo de datos](#modelo-de-datos)
- [Seguridad (JWT + roles)](#seguridad-jwt--roles)
- [Frontend Angular](#frontend-angular)
- [Puesta en marcha con Docker](#puesta-en-marcha-con-docker)
- [Ejecución en local (IntelliJ)](#ejecución-en-local-intellij)
- [API — resumen de endpoints](#api--resumen-de-endpoints)
- [Diagramas](#diagramas)

---

## Arquitectura

Cada microservicio es autónomo: tiene su propio puerto, su base de datos (si
la necesita) y su ciclo de vida. No comparten tablas. Todo el tráfico entra
por un único **gateway** (Spring Cloud Gateway, sabor reactivo WebFlux) que
enruta cada prefijo `/api/<área>/**` al microservicio correspondiente sin
reescribir la ruta, y reenvía la cabecera `Authorization` tal cual. Cuando un
servicio necesita datos de otro (por ejemplo, `surveys` necesita el nombre
del propietario, o `responses` necesita releer el esquema de preguntas), hace
una **llamada HTTP reenviando el token JWT** del usuario, en lugar de acceder
a la base de datos ajena.

| Servicio        | Puerto | Base de datos          | Responsabilidad                                        |
|------------------|:------:|-------------------------|---------------------------------------------------------|
| `gateway`        | 8080   | —                        | Punto de entrada único, enrutado estático              |
| `users`          | 8081   | `usersurveyDB`           | Registro, login, emisión de JWT, roles                 |
| `surveys`        | 8082   | `surveysDB`              | Encuestas, preguntas, opciones, publicación/cierre      |
| `responses`      | 8083   | `responsesDB`            | Envío de respuestas, validación, resultados agregados   |
| `statistics`     | 8084   | — (consume `responses`)  | Estadísticas y series temporales de una encuesta        |
| `invitations`    | 8085   | `invitationsSurveyDB`     | Invitaciones por email, tokens de acceso                |
| `notifications`  | 8086   | — (SMTP)                 | Envío de correos                                        |
| *frontend*       | 4200   | —                        | Aplicación Angular (fuera de Docker por defecto)        |

Una **única instancia de PostgreSQL** aloja las cuatro bases de datos con
persistencia (creadas por `init-databases.sql` la primera vez que arranca).

---

## Stack tecnológico

**Backend**
- Java 21
- Spring Boot 4 (Web MVC, Data JPA, Validation, Security)
- Spring Cloud Gateway (WebFlux) para el `gateway`
- Spring Security como **OAuth2 Resource Server** (validación de JWT, HMAC-SHA256)
- `io.jsonwebtoken` (jjwt) para la emisión del token en `users`
- PostgreSQL 16
- springdoc-openapi (Swagger UI)
- Apache HttpClient 5 (`RestTemplate` entre servicios, con interceptor que reenvía el JWT)
- Spring Scheduling (`SurveyAutoCloseTask`) para el cierre automático de encuestas
- Spring Mail (SMTP) en `notifications`

**Frontend**
- Angular (componentes *standalone*)
- Interceptor HTTP para adjuntar el JWT (`auth-interceptor`) y `AuthGuard` por rol
- RxJS

**Infraestructura**
- Docker Compose (Postgres + 6 microservicios; el frontend se ejecuta aparte con `ng serve`)

---

## Estructura del repositorio

```
eusurvey/
├── docker-compose.yml         # Postgres + 6 microservicios (gateway incluido)
├── init-databases.sql         # crea las 4 bases de datos con persistencia
│
├── gateway/                   # Spring Cloud Gateway (WebFlux), sin base de datos
│   ├── pom.xml
│   └── src/main/java/com/igarciamen/gateway/...
├── users/                     # cada microservicio: pom.xml + src/
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
└── frontend/                  # Angular (se ejecuta con ng serve)
    └── src/app/...
```

Paquete base de cada servicio: `com.igarciamen.<servicio>`, con la
organización habitual por capas: `model`, `repository`, `payloads` (DTOs de
petición/respuesta), `service`, `controller`, `config`, y `client` (en los
servicios que llaman a otros: `surveys`→`users`, `responses`→`surveys`,
`statistics`→`responses`, `invitations`→`surveys`/`notifications`).

---

## Los microservicios

### gateway (8080)
Único punto de entrada. Tabla de rutas estática (`GatewayConfig`): cada
prefijo `/api/<área>/**` se reenvía sin cambios al microservicio propietario
(`/api/auth/**` y `/api/user(s)/**` → `users`; `/api/surveys/**` → `surveys`;
`/api/responses/**` → `responses`; `/api/statistics/**` → `statistics`;
`/api/invitations/**` → `invitations`). No valida el JWT él mismo; se limita a
reenviar la cabecera `Authorization`, y es cada microservicio el que la valida
como *Resource Server*.

### users (8081)
Gestiona la identidad. Registro (`/api/auth/signup`) y login
(`/api/auth/login`), que devuelve un **JWT firmado con HMAC-SHA256** con los
claims `userId` y `roles`. Expone también los datos del usuario autenticado
(`/api/user/me`) y de un usuario por id (`/api/user/{id}`, usado por otros
servicios para enriquecer sus respuestas). Roles disponibles: `ROLE_USER`,
`ROLE_DESIGNER`, `ROLE_ADMIN`. Semilla un usuario administrador al arrancar
(`DataLoader`, configurable por `app.admin.username` / `app.admin.password`).

### surveys (8082)
CRUD de encuestas con su árbol de preguntas y opciones
(`Survey 1—N Question 1—N QuestionOption`). Ciclo de vida controlado por
`SurveyStatus` (`DRAFT → PUBLISHED → CLOSED`, transición unidireccional): solo
se puede editar en `DRAFT`, y publicar exige al menos una pregunta. Controla
el acceso con `SurveyAccess` (`OPEN` / `PASSWORD` / `RESTRICTED`); la
contraseña se guarda en texto y **nunca** se serializa de vuelta al cliente
(`@JsonProperty(WRITE_ONLY)`). Expone un endpoint interno de gestión
(`/{id}/manage`) que exige ser el propietario o admin, usado por
`invitations`. Enriquece cada encuesta con los datos del propietario llamando
a `users`. Incluye una tarea programada (`SurveyAutoCloseTask` +
`SchedulingConfig`) que cierra automáticamente las encuestas cuya
`closesAt` ya pasó.

### responses (8083)
Recibe el envío de una respuesta (`POST /api/responses/survey/{id}`). Antes de
aceptarla: vuelve a pedir el esquema publicado a `surveys` (no confía en lo
que envía el cliente), aplica las reglas del modo de acceso (`RESTRICTED`
exige login; `PASSWORD` exige login + contraseña correcta verificada contra
`surveys`; `OPEN` admite anónimos), impide una segunda respuesta del mismo
usuario registrado a la misma encuesta, y valida cada respuesta contra el tipo
de pregunta (obligatoriedad, opciones válidas, etc.). También calcula
resultados agregados por pregunta (`/results`), las marcas de tiempo de cada
envío (`/timestamps`, usado por `statistics`) y permite exportar las
respuestas en **CSV** y **Excel** (owner o admin).

### statistics (8084, sin base de datos)
Servicio de solo lectura y agregación: pide a `responses` los resultados y las
marcas de tiempo de una encuesta y construye una vista con el total de
respuestas, primera/última respuesta, una serie temporal por día, y un
*highlight* por pregunta (opción más elegida, media, sí/no, o número de
respuestas de texto libre).

### invitations (8085)
Invita por email a una encuesta. Al crear invitaciones valida que quien las
pide sea el propietario/admin de la encuesta (`GET /{id}/manage` en
`surveys`), limpia y deduplica los correos, genera un token único por
invitación, y pide a `notifications` el envío del email con el enlace
(`{frontend}/surveys/{id}/answer?invite={token}`); si el email falla, la
invitación igualmente queda creada (best-effort). El destinatario puede
validar su token (`GET /token/{token}`) y marcarlo como respondido
(`POST /token/{token}/accept`).

### notifications (8086, sin base de datos)
Servicio interno de envío de correo por SMTP. Expone
`POST /api/notifications/invitation`, pensado para ser llamado solo por
`invitations`.

---

## Modelo de datos

Entidades principales (ver diagramas en [`UML.md`](./UML.md)):

- **User** ⟷ **Role** (`ManyToMany`), `Role.name` es el enum **ERole**.
- **Survey** *1—N* **Question** *1—N* **QuestionOption** (FK reales dentro de `surveysDB`).
- **SurveyResponse** *1—N* **Answer** (FK reales dentro de `responsesDB`); cada
  `Answer` guarda un valor simple (`textValue`) o una lista de ids de opciones
  elegidas (`selectedOptionIds`), según el tipo de pregunta.
- **Invitation** (tabla única dentro de `invitationsSurveyDB`, sin hijos).

Como cada servicio tiene su base de datos, **no hay claves foráneas entre
servicios**: las referencias cruzadas (por ejemplo `Survey.ownerUserId`,
`SurveyResponse.surveyId`, `Answer.questionId`, `Invitation.surveyId`) son
identificadores lógicos que se resuelven por HTTP o se validan en el momento
de escribir.

---

## Seguridad (JWT + roles)

- `users` emite el JWT en el login, firmado con una clave secreta HMAC
  compartida (`jwt.secret`), con los claims `userId` y `roles`.
- El resto de servicios actúan como **Resource Server**: validan el token con
  la misma clave (`NimbusJwtDecoder`, HmacSHA256). Los roles se leen del
  *claim* `roles` sin prefijo añadido.
- El `gateway` no valida el JWT: solo enruta y reenvía la cabecera
  `Authorization` sin modificarla.
- El frontend guarda el token y lo añade a cada petición mediante un
  **interceptor** (`Authorization: Bearer ...`). Entre servicios, el token se
  **reenvía** con un interceptor de `RestTemplate` (`RestTemplateConfig`) que
  copia la cabecera `Authorization` de la petición entrante a la saliente.
- Reglas típicas: catálogo de encuestas publicadas y respuesta a encuestas
  `OPEN` son públicos; crear/editar/publicar/cerrar/borrar una encuesta,
  ver resultados/estadísticas e invitar exige ser el propietario o `ROLE_ADMIN`;
  Swagger y `/api/auth/**` son públicos.

---

## Frontend Angular

Aplicación *standalone* con enrutado protegido por `AuthGuard` y por rol
(`ROLE_DESIGNER` / `ROLE_ADMIN` según la ruta). Rutas principales:

```
/login  /signup           acceso

/surveys                  catálogo público de encuestas publicadas
/surveys/:id               detalle de una encuesta
/surveys/:id/answer         responder (pública; RESTRICTED exige login)

/surveys/new                crear encuesta (DESIGNER/ADMIN)
/surveys/manage              gestionar mis encuestas (DESIGNER/ADMIN)
/surveys/:id/edit            editar encuesta en DRAFT (DESIGNER/ADMIN)
/surveys/:id/results          resultados agregados (propietario/ADMIN)
/surveys/:id/statistics        estadísticas (propietario/ADMIN)
/surveys/:id/invitations        invitaciones (propietario/ADMIN)
```

---

## Puesta en marcha con Docker

Requisitos: Docker y Docker Compose.

```bash
# Desde la carpeta raíz (donde está docker-compose.yml)
docker compose up --build
```

La primera vez compila las seis imágenes y crea las bases de datos. Cuando
PostgreSQL esté *healthy*, arrancan `users`, `surveys`, `responses`,
`statistics`, `invitations` y, por último, `gateway` (que depende de todos
los anteriores).

Comprobación rápida (Swagger de cada servicio):

```
http://localhost:8081/swagger-ui.html   # users
http://localhost:8082/swagger-ui.html   # surveys
http://localhost:8085/swagger-ui.html   # invitations
```

Todo el tráfico de la aplicación puede pasar por el gateway:

```
http://localhost:8080/api/...
```

Frontend (fuera de Docker):

```bash
cd frontend
npm install
ng serve
# http://localhost:4200
```

Variables de entorno relevantes (ver `docker-compose.yml` y `_env`):
`MAIL_USERNAME` / `MAIL_PASSWORD` (SMTP de `notifications`),
`FRONTEND_BASE_URL` (usado por `invitations` para construir el enlace de
invitación).

Parada y persistencia:

```bash
docker compose down       # conserva los datos (volumen postgres-data)
docker compose down -v    # borra también los datos (empezar de cero)
docker compose logs -f surveys   # ver logs de un servicio
```

Las URLs entre contenedores y la conexión a la base de datos se inyectan por
**variables de entorno** en `docker-compose.yml`, por lo que los
`application.properties` (que usan `localhost`) no se tocan y siguen sirviendo
para ejecutar desde IntelliJ.

---

## Ejecución en local (IntelliJ)

1. Arranca un PostgreSQL local y crea las cuatro bases de datos
   (`usersurveyDB`, `surveysDB`, `responsesDB`, `invitationsSurveyDB`).
2. Ajusta credenciales en cada `application.properties` si difieren.
3. Ejecuta cada microservicio como aplicación Spring Boot (incluido `gateway`).
4. `ng serve` para el frontend.

---

## API — resumen de endpoints

Todos accesibles a través del gateway en `http://localhost:8080`.

**users**
```
POST /api/auth/signup
POST /api/auth/login
GET  /api/user/me
GET  /api/user/{id}
```

**surveys**
```
GET    /api/surveys?q=                     (público, paginado)
GET    /api/surveys/{id}                   (público, solo PUBLISHED)
GET    /api/surveys/mine                   (propietario)
GET    /api/surveys/{id}/manage            (propietario/ADMIN, cualquier estado)
POST   /api/surveys                        (propietario)
PUT    /api/surveys/{id}                   (propietario/ADMIN, solo DRAFT)
PATCH  /api/surveys/{id}/status             (propietario/ADMIN; DRAFT→PUBLISHED→CLOSED)
DELETE /api/surveys/{id}                   (propietario/ADMIN)
POST   /api/surveys/{id}/verify-password    (público)
```

**responses**
```
POST /api/responses/survey/{surveyId}                  (público u opcional, según access)
GET  /api/responses/survey/{surveyId}/mine              (autenticado)
GET  /api/responses/survey/{surveyId}/results            (propietario/ADMIN)
GET  /api/responses/survey/{surveyId}/timestamps          (propietario/ADMIN)
GET  /api/responses/survey/{surveyId}/export/csv           (propietario/ADMIN)
GET  /api/responses/survey/{surveyId}/export/xlsx            (propietario/ADMIN)
```

**statistics**
```
GET /api/statistics/survey/{surveyId}       (propietario/ADMIN)
```

**invitations**
```
POST   /api/invitations/survey/{surveyId}                          (propietario/ADMIN)
GET    /api/invitations/survey/{surveyId}                          (propietario/ADMIN)
GET    /api/invitations/token/{token}                              (autenticado)
POST   /api/invitations/token/{token}/accept                       (autenticado)
DELETE /api/invitations/{invitationId}                             (propietario/ADMIN)
```

**notifications** (uso interno)
```
POST /api/notifications/invitation
```

---

## Diagramas

Los diagramas UML (clases del dominio, arquitectura de contenedores, flujos
de autenticación y de envío de respuestas, y modelo entidad-relación) están en
[`UML.md`](./UML.md), en formato Mermaid. Se renderizan en GitHub/GitLab, en
VS Code con la extensión de Mermaid, o en https://mermaid.live.

También están disponibles como archivos `.mmd` sueltos, listos para
importar o pegar directamente en un visor Mermaid:

- [`class-diagram.mmd`](./class-diagram.mmd) — clases del dominio (JPA)
- [`architecture-c4.mmd`](./architecture-c4.mmd) — contenedores (C4 nivel 2)
- [`auth-sequence.mmd`](./auth-sequence.mmd) — flujo de autenticación (JWT)
- [`checkout-sequence.mmd`](./checkout-sequence.mmd) — flujo de envío de una respuesta
- [`er-diagram.mmd`](./er-diagram.mmd) — modelo entidad-relación

---

## Licencia

Proyecto académico. Uso educativo.
