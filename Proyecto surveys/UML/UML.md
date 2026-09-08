# Diagramas del sistema — EUSurvey (Spring Boot 4 + Angular, microservicios)

Este documento contiene los diagramas del proyecto en formato **Mermaid**.
Se renderizan automáticamente en GitHub, GitLab, VS Code (con extensión Mermaid),
o en https://mermaid.live.

## Cómo leer estos diagramas (niveles de abstracción)

Un sistema de microservicios se documenta con **varios diagramas
complementarios**, cada uno en su nivel. Mezclarlos en uno solo es un error
conceptual habitual. Aquí se sigue la separación estándar (inspirada en el
**modelo C4**):

| Diagrama | Nivel | Qué muestra | Qué NO muestra |
|----------|-------|-------------|----------------|
| **1. Clases del dominio** | Diseño | Entidades JPA y sus atributos; referencias entre servicios como asociaciones *punteadas* | Infraestructura |
| **2. Contenedores (C4-2)** | Arquitectura | Los microservicios, el gateway, sus bases de datos y la **comunicación HTTP/REST** entre ellos | Detalle de tablas |
| **3. Flujo de autenticación** | Comportamiento | Login y validación del JWT paso a paso | Estructura estática |
| **4. Flujo de envío de una respuesta** | Comportamiento | Cómo se responde una encuesta, con las validaciones de acceso | Estructura estática |
| **5. Entidad-relación (ER)** | Modelo físico | Tablas, columnas y **claves foráneas reales dentro de cada base de datos** | Comunicación entre servicios |

**Decisión de diseño clave (importante para entender los diagramas):** cada
microservicio con persistencia tiene su **propia base de datos** dentro de la
misma instancia de PostgreSQL (`usersurveyDB`, `surveysDB`, `responsesDB`,
`invitationsSurveyDB`). Por tanto, **no existen claves foráneas entre
servicios**. Una columna como `surveys.owner_user_id` o
`survey_responses.survey_id` es solo un identificador lógico (`Long`) que
apunta a un dato que vive en la base de datos de otro servicio; la integridad
**no** la garantiza la base de datos, sino una **llamada REST en tiempo de
ejecución** (con `RestTemplate`, reenviando el JWT del usuario) o la
validación en el momento de escribir.

Consecuencia práctica:
- En el **diagrama de contenedores** esas conexiones SÍ aparecen (son llamadas
  HTTP): `surveys → users`, `responses → surveys`, `statistics → responses`,
  `invitations → surveys`, `invitations → notifications`.
- En el **diagrama ER** esas conexiones NO aparecen como relación, porque no
  son claves foráneas reales. Que `surveys`, `responses` e `invitations`
  salgan "sueltos" entre sí es **correcto**, no un olvido.
- En el **diagrama de clases** aparecen como líneas **punteadas** (`..>`) para
  indicar "dependencia lógica por id, no relación de datos".
- Además, aquí **no hay un servicio de negocio de dos pasos** como el
  checkout de un e-commerce (descontar stock y poder revertirlo); el flujo
  equivalente más rico es el **envío de una respuesta**, porque combina una
  llamada de solo lectura a otro servicio (`responses → surveys`) con
  validaciones de negocio (modo de acceso, duplicados, esquema de preguntas).

---

## 1. Diagrama de clases del dominio (entidades JPA)

```mermaid
classDiagram
    direction LR

    %% ---------- users ----------
    class User {
        +Long id
        +String username
        +String email
        +String password
        +Set~Role~ roles
    }
    class Role {
        +Integer id
        +ERole name
    }
    class ERole {
        <<enumeration>>
        ROLE_USER
        ROLE_DESIGNER
        ROLE_ADMIN
    }
    User "1" *-- "many" Role : roles (user_roles)
    Role --> ERole : name

    %% ---------- surveys ----------
    class Survey {
        +Long id
        +Long ownerUserId
        +String title
        +String description
        +SurveyStatus status
        +SurveyAccess access
        +String accessPassword
        +Instant publishedAt
        +Instant closesAt
        +Instant createdAt
        +Instant updatedAt
    }
    class SurveyStatus {
        <<enumeration>>
        DRAFT
        PUBLISHED
        CLOSED
    }
    class SurveyAccess {
        <<enumeration>>
        OPEN
        PASSWORD
        RESTRICTED
    }
    class Question {
        +Long id
        +QuestionType type
        +String label
        +String helpText
        +boolean required
        +Integer position
    }
    class QuestionType {
        <<enumeration>>
        TEXT
        TEXTAREA
        NUMBER
        DATE
        BOOLEAN
        SINGLE_CHOICE
        MULTIPLE_CHOICE
        DROPDOWN
        RATING
    }
    class QuestionOption {
        +Long id
        +String label
        +String value
        +Integer position
    }
    Survey "1" *-- "many" Question : questions
    Question "1" *-- "many" QuestionOption : options
    Survey --> SurveyStatus : status
    Survey --> SurveyAccess : access
    Question --> QuestionType : type
    Survey ..> User : ownerUserId

    %% ---------- responses ----------
    class SurveyResponse {
        +Long id
        +Long surveyId
        +Long respondentUserId
        +Instant submittedAt
    }
    class Answer {
        +Long id
        +Long questionId
        +String textValue
        +List~Long~ selectedOptionIds
    }
    SurveyResponse "1" *-- "many" Answer : answers
    SurveyResponse ..> Survey : surveyId
    SurveyResponse ..> User : respondentUserId
    Answer ..> Question : questionId
    Answer ..> QuestionOption : selectedOptionIds

    %% ---------- invitations ----------
    class Invitation {
        +Long id
        +Long surveyId
        +String surveyTitle
        +Long ownerUserId
        +String email
        +String token
        +InvitationStatus status
        +Instant createdAt
        +Instant respondedAt
        +Long respondentUserId
    }
    class InvitationStatus {
        <<enumeration>>
        PENDING
        RESPONDED
    }
    Invitation --> InvitationStatus : status
    Invitation ..> Survey : surveyId
    Invitation ..> User : ownerUserId / respondentUserId

    %% ---- Estilos: paleta de contraste, sin naranja ----
    style User fill:#c7d2fe,stroke:#4f46e5,stroke-width:2px,color:#1e1b4b
    style Role fill:#c7d2fe,stroke:#4f46e5,stroke-width:2px,color:#1e1b4b
    style ERole fill:#e0e7ff,stroke:#6366f1,stroke-width:1px,color:#312e81

    style Survey fill:#bbf7d0,stroke:#16a34a,stroke-width:2px,color:#052e16
    style Question fill:#bbf7d0,stroke:#16a34a,stroke-width:2px,color:#052e16
    style QuestionOption fill:#bbf7d0,stroke:#16a34a,stroke-width:2px,color:#052e16
    style SurveyStatus fill:#dcfce7,stroke:#22c55e,stroke-width:1px,color:#14532d
    style SurveyAccess fill:#dcfce7,stroke:#22c55e,stroke-width:1px,color:#14532d
    style QuestionType fill:#dcfce7,stroke:#22c55e,stroke-width:1px,color:#14532d

    style SurveyResponse fill:#bae6fd,stroke:#0284c7,stroke-width:2px,color:#0c2233
    style Answer fill:#bae6fd,stroke:#0284c7,stroke-width:2px,color:#0c2233

    style Invitation fill:#fbcfe8,stroke:#db2777,stroke-width:2px,color:#500724
    style InvitationStatus fill:#fce7f3,stroke:#ec4899,stroke-width:1px,color:#831843
```

> **Nota sobre `Survey.owner`:** es un `Map<String,Object>` `@Transient` que
> **no se persiste**; se rellena en tiempo de ejecución con la respuesta de
> `GET /api/user/{id}` en `users` (mismo patrón que `Product.seller` en un
> e-commerce). Por eso no aparece como columna en el ER. `accessPassword` es
> `@JsonProperty(WRITE_ONLY)`: se puede enviar al crear/editar, pero nunca se
> devuelve al cliente.

---

## 2. Diagrama de contenedores (C4 nivel 2)

Vista de arquitectura. Un **gateway** (Spring Cloud Gateway, WebFlux) como
único punto de entrada, y cinco microservicios Spring Boot independientes
(cuatro con base de datos propia, uno —`statistics`— sin base de datos, y
`notifications` sin base de datos), más el frontend Angular. **Las flechas
etiquetadas `[REST/JSON]` son llamadas HTTP** (con `RestTemplate`, reenviando
el JWT recibido), no relaciones de base de datos.

```mermaid
flowchart TB
    User(["Usuario"])

    subgraph Client["Frontend"]
        FE["<b>Angular SPA</b><br/>[Container: TypeScript]<br/>ng serve :4200"]
    end

    subgraph GW["Gateway"]
        GATE["<b>gateway</b> :8080<br/>Spring Cloud Gateway (WebFlux)<br/>punto de entrada único"]
    end

    subgraph Services["Microservicios (Spring Boot · Java 21)"]
        USERS["<b>users</b> :8081<br/>Auth, JWT, roles"]
        SURV["<b>surveys</b> :8082<br/>Encuestas, preguntas, opciones"]
        RESP["<b>responses</b> :8083<br/>Respuestas, resultados agregados"]
        STAT["<b>statistics</b> :8084<br/>Estadísticas (sin BD)"]
        INV["<b>invitations</b> :8085<br/>Invitaciones, tokens"]
        NOTIF["<b>notifications</b> :8086<br/>Envío de email (SMTP, sin BD)"]
    end

    subgraph Data["PostgreSQL 16 :5433 (contenedor único)"]
        DB1[("usersurveyDB")]
        DB2[("surveysDB")]
        DB3[("responsesDB")]
        DB5[("invitationsSurveyDB")]
    end

    User -->|HTTPS| FE
    FE -->|"login / encuestas / respuestas<br/>estadísticas / invitaciones<br/>[REST/JSON + Bearer JWT]"| GATE

    GATE -->|"/api/auth/**, /api/user/**"| USERS
    GATE -->|"/api/surveys/**"| SURV
    GATE -->|"/api/responses/**"| RESP
    GATE -->|"/api/statistics/**"| STAT
    GATE -->|"/api/invitations/**"| INV

    USERS -->|JDBC| DB1
    SURV -->|JDBC| DB2
    RESP -->|JDBC| DB3
    INV -->|JDBC| DB5

    SURV -->|"GET /api/user/{ownerUserId}<br/>[REST/JSON · enriquece 'owner']"| USERS
    RESP -->|"GET /api/surveys/{id}<br/>POST /api/surveys/{id}/verify-password<br/>[REST/JSON]"| SURV
    STAT -->|"GET /api/responses/survey/{id}/results<br/>GET /api/responses/survey/{id}/timestamps<br/>[REST/JSON]"| RESP
    INV -->|"GET /api/surveys/{id}/manage<br/>[REST/JSON · reenvía Bearer]"| SURV
    INV -->|"POST /api/notifications/invitation<br/>[REST/JSON]"| NOTIF

    %% ---- Contenedores (subgraphs): fondo oscuro neutro, nada de naranja ----
    style Client fill:#0f172a,stroke:#38bdf8,stroke-width:1px,color:#e2e8f0
    style GW fill:#0f172a,stroke:#a78bfa,stroke-width:1px,color:#e2e8f0
    style Services fill:#111827,stroke:#4b5563,stroke-width:1px,color:#e5e7eb
    style Data fill:#0f172a,stroke:#f59e0b,stroke-width:1px,color:#e2e8f0

    %% ---- Nodos: colores pastel con texto oscuro, alto contraste ----
    classDef fe fill:#bae6fd,stroke:#0284c7,stroke-width:2px,color:#0c2233;
    classDef gw fill:#ddd6fe,stroke:#7c3aed,stroke-width:2px,color:#2e1065;
    classDef svc fill:#bbf7d0,stroke:#16a34a,stroke-width:2px,color:#052e16;
    classDef db fill:#fde68a,stroke:#b45309,stroke-width:2px,color:#451a03;
    classDef userNode fill:#c7d2fe,stroke:#4f46e5,stroke-width:2px,color:#1e1b4b;

    class FE fe;
    class GATE gw;
    class USERS,SURV,RESP,STAT,INV,NOTIF svc;
    class DB1,DB2,DB3,DB5 db;
    class User userNode;
```

> **Leyenda:** flecha continua `[REST/JSON]` = llamada HTTP síncrona entre
> servicios (reenvía el JWT recibido en `Authorization`, vía el interceptor de
> `RestTemplateConfig`). `JDBC` = acceso de cada servicio a su **propia** base
> de datos. No hay accesos cruzados a bases de datos ajenas. A diferencia de
> un despliegue con `Eureka`/`service discovery`, aquí el `gateway` usa un
> **enrutado estático** (`GatewayConfig`) con las URLs de cada servicio
> inyectadas por variables de entorno.

---

## 3. Flujo de autenticación (JWT como Resource Server)

`users` emite el token al hacer login (con los claims `userId` y `roles`); el
resto de servicios lo validan como **OAuth2 Resource Server** con la misma
clave secreta HMAC (`NimbusJwtDecoder`, HmacSHA256). El token viaja en la
cabecera `Authorization: Bearer ...`, pasa a través del gateway sin
modificarse, y se reenvía entre servicios cuando uno necesita llamar a otro.

```mermaid
sequenceDiagram
    autonumber
    participant U as Usuario (Angular)
    participant GW as gateway :8080
    participant AU as users :8081
    participant SU as surveys :8082

    U->>GW: POST /api/auth/login (username, password)
    GW->>AU: forward POST /api/auth/login
    AU-->>GW: 200 { token JWT (userId, roles) }
    GW-->>U: 200 { token JWT }
    Note over U: El interceptor guarda el token<br/>y lo añade a cada petición

    U->>GW: POST /api/surveys (Bearer JWT)
    GW->>SU: forward (Authorization: Bearer ...)
    SU->>SU: Valida JWT (Resource Server, misma clave HMAC)
    SU-->>GW: 201 Encuesta creada
    GW-->>U: 201 Encuesta creada
```

---

## 4. Flujo de envío de una respuesta a una encuesta

Equivalente al "checkout": es el flujo de negocio más rico del sistema.
`responses` no confía en lo que le manda el cliente sobre la encuesta —vuelve
a pedir el esquema publicado a `surveys` y, si hace falta, valida la
contraseña— antes de aceptar la respuesta.

```mermaid
sequenceDiagram
    autonumber
    participant U as Usuario (Angular)
    participant GW as gateway :8080
    participant SU as surveys :8082
    participant RE as responses :8083

    U->>GW: GET /api/surveys/{id} (ver encuesta publicada)
    GW->>SU: forward
    SU-->>GW: 200 Survey (title, questions, options, access)
    GW-->>U: 200 Survey

    opt access = PASSWORD
        U->>GW: POST /api/surveys/{id}/verify-password
        GW->>SU: forward
        SU-->>GW: 200 { valid: true/false }
        GW-->>U: 200 { valid }
    end

    U->>GW: POST /api/responses/survey/{id} (Bearer JWT opcional, answers[])
    GW->>RE: forward
    RE->>SU: GET /api/surveys/{id} (releer schema publicado)
    SU-->>RE: 200 Survey

    RE->>RE: Valida modo de acceso (OPEN/PASSWORD/RESTRICTED)
    alt access = PASSWORD
        RE->>SU: POST /api/surveys/{id}/verify-password
        SU-->>RE: 200 { valid }
    end
    RE->>RE: Valida respuestas contra el schema<br/>(obligatoriedad, tipos, opciones válidas)
    RE->>RE: Comprueba duplicado (survey_id + respondent_user_id)
    RE-->>GW: 201 SurveyResponse creada
    GW-->>U: 201 Respuesta enviada
```

> **Nota:** si la encuesta es `RESTRICTED`, el paso de acceso público
> `GET /api/surveys/{id}` sigue devolviendo la encuesta (es de solo lectura),
> pero el envío (`POST /api/responses/survey/{id}`) exige un JWT válido; sin
> token, `responses` responde `401`. Con `PASSWORD`, además se exige estar
> autenticado y enviar `accessPassword` en el cuerpo.

---

## 5. Modelo entidad-relación (por base de datos)

Cada recuadro pertenece a una **base de datos independiente** dentro de la
misma instancia de PostgreSQL. Solo se dibujan las **claves foráneas reales**
(las que existen dentro de una misma base). Por diseño, `SURVEYS`,
`SURVEY_RESPONSES` e `INVITATIONS` **no** están conectados entre sí en este
diagrama: sus referencias (`survey_id`, `owner_user_id`,
`respondent_user_id`...) son identificadores lógicos hacia otras bases,
resueltos por HTTP, **no** claves foráneas. Que aparezcan como tablas sueltas
es lo correcto en un ER; la comunicación entre ellas se ve en el **diagrama de
contenedores (nº 2)**.

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ USER_ROLES : in
    USERS {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password
    }
    ROLES {
        int id PK
        varchar name UK
    }
    USER_ROLES {
        bigint user_id FK
        bigint role_id FK
    }

    SURVEYS ||--o{ SURVEY_QUESTIONS : contains
    SURVEY_QUESTIONS ||--o{ SURVEY_QUESTION_OPTIONS : contains
    SURVEYS {
        bigint id PK
        bigint owner_user_id
        varchar title
        varchar description
        varchar status
        varchar access
        varchar access_password
        timestamp published_at
        timestamp closes_at
        timestamp created_at
        timestamp updated_at
    }
    SURVEY_QUESTIONS {
        bigint id PK
        bigint survey_id FK
        varchar type
        varchar label
        varchar help_text
        boolean required
        int position
    }
    SURVEY_QUESTION_OPTIONS {
        bigint id PK
        bigint question_id FK
        varchar label
        varchar value
        int position
    }

    SURVEY_RESPONSES ||--o{ ANSWERS : contains
    ANSWERS ||--o{ ANSWER_SELECTED_OPTIONS : contains
    SURVEY_RESPONSES {
        bigint id PK
        bigint survey_id
        bigint respondent_user_id
        timestamp submitted_at
    }
    ANSWERS {
        bigint id PK
        bigint response_id FK
        bigint question_id
        varchar text_value
    }
    ANSWER_SELECTED_OPTIONS {
        bigint answer_id FK
        bigint option_id
    }

    INVITATIONS {
        bigint id PK
        bigint survey_id
        varchar survey_title
        bigint owner_user_id
        varchar email
        varchar token UK
        varchar status
        timestamp created_at
        timestamp responded_at
        bigint respondent_user_id
    }
```

> **Restricciones únicas relevantes (no siempre visibles en un ER simple):**
> `survey_responses` tiene una clave única compuesta
> `(survey_id, respondent_user_id)` — una persona registrada solo puede
> responder una vez a cada encuesta (las respuestas anónimas no se
> deduplican). `invitations` tiene `token` único y una clave única compuesta
> `(survey_id, email)` — no se invita dos veces al mismo correo a la misma
> encuesta.
