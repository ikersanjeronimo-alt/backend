# Modelo Entidad-Relación — ShareYourStory

Modelo de datos real generado por Hibernate a partir de las entidades JPA
(`src/main/java/shareyourstory/domain/**/model`).

## Diagrama E/R

```mermaid
erDiagram
    PROFESSIONS      ||--o{ USERS              : "clasifica"
    SPECIALIZATIONS  ||--o{ USERS              : "clasifica"
    USERS            ||--o{ VALORATIONS        : "recibe"
    STORYMAPS        ||--o{ REPORTS            : "reportada (FK story_id)"
    USERS            ||--o{ REPORTS            : "resuelve (FK resolved_by)"
    REPORTS          ||..o{ REPORT_AUDIT       : "audita (lógica + trigger)"
    USERS            ||..o{ COMMUNITY_MEMBERS  : "es miembro (lógica, sin FK)"
    COMMUNITIES      ||..o{ COMMUNITY_MEMBERS  : "tiene miembro (lógica, sin FK)"
    USERS            ||..o{ PRIVATE_MESSAGES   : "participa (lógica, sin FK)"
    COMMUNITIES      ||..o{ COMMUNITY_MESSAGES : "contiene (lógica, sin FK)"
    USERS            ||..o{ COMMUNITY_MESSAGES : "escribe (lógica, sin FK)"

    USERS {
        int     userId PK
        string  name
        string  lastName
        string  userName  UK "NOT NULL"
        string  nickName  UK "NOT NULL"
        string  userPassword "NOT NULL (BCrypt)"
        string  companyName
        string  mail      UK
        date    creationDate "NOT NULL"
        string  profession "texto libre"
        string  specialization "texto libre"
        int     professionId FK "→ professions (muerta)"
        int     specializationId FK "→ specializations (muerta)"
        enum    role "ANON|USER|PROFESSIONAL|ADMINISTRATOR"
        string  secretKey UK "TOTP 2FA"
        bool    twoFactorEnabled "NOT NULL"
        string  topics "CSV de temas (onboarding)"
        int     warnings "NOT NULL (moderación)"
        bool    banned "NOT NULL (moderación)"
    }
    PROFESSIONS {
        int     professionId PK
        string  professionDescription UK "NOT NULL"
    }
    SPECIALIZATIONS {
        int     specializationId PK
        string  specializationDescription UK "NOT NULL"
    }
    VALORATIONS {
        int     valorationId PK
        int     valorationPoint "NOT NULL"
        date    valorationDate "NOT NULL"
        int     userId FK "NOT NULL"
    }
    PRIVATE_MESSAGES {
        bigint  id PK
        int     userId "lógica → USERS"
        int     professionalId "lógica → USERS"
        string  text
        datetime createdAt
        string  from_role "user|professional"
    }
    COMMUNITIES {
        bigint  id PK
        string  name
        string  emoji
        string  moderator "nombre visible (col 'moderator')"
        int     modUserId "userId del moderador"
        string  description
        enum    category "CommunityTypes"
        int     members
        int     online
        string  pinnedNote
        bool    joined
        bool    chatClosed
    }
    COMMUNITY_MEMBERS {
        bigint  id PK
        int     userId "NOT NULL, UNIQUE(userId,communityId)"
        bigint  communityId "NOT NULL"
    }
    COMMUNITY_MESSAGES {
        bigint  id PK
        int     communityId "lógica → COMMUNITIES"
        int     userId "lógica → USERS"
        string  username
        string  text
        datetime createdAt
    }
    REPORTS {
        int     id PK
        enum    target_type "STORY|MESSAGE|PRIVATE_MESSAGE, NOT NULL"
        int     story_id FK "→ STORYMAPS (nullable)"
        bigint  message_id "lógica → mensaje (com/privado)"
        string  reason "NOT NULL"
        enum    status "PENDING|RESOLVED|DISMISSED"
        string  content
        string  reported_username
        int     reporter_id
        string  reporter_username
        string  community
        datetime created_at "NOT NULL"
        datetime resolved_at
        int     resolved_by FK "→ USERS (moderador)"
    }
    REPORT_AUDIT {
        int     id PK
        int     report_id "lógica → REPORTS"
        int     moderator_id
        string  old_status
        string  new_status
        string  action
        datetime created_at
    }
    STORYMAPS {
        int     id PK
        string  message
        double  latitude
        double  longitude
    }
    TIMEMACHINE {
        int     id PK
        string  message
        string  email "@Email"
        date    deliveryDate
    }
    BOTTLES {
        int     id PK
        string  message "NOT BLANK"
        int     author_id
        bool    received "NOT NULL"
        datetime created_at "NOT NULL"
    }
    EVENTS {
        int     id PK
        string  title
        string  date
        string  place
        string  description
        string  topic
        int     reaction "contador 'Me interesa' global"
    }
```

## Entidades

| Tabla | Entidad JPA | Descripción |
|---|---|---|
| `users` | `User` | Cuentas (anónimas, usuarios, profesionales, administradores). Implementa `UserDetails`. Incluye 2FA (`secretKey`/`twoFactorEnabled`), `topics` (onboarding) y moderación (`warnings`/`banned`). |
| `professions` | `Profession` | Catálogo de profesiones. **Entidad muerta** (FK desde `users`, sin uso real — ver Roadmap). |
| `specializations` | `Specialization` | Catálogo de especializaciones. **Entidad muerta** (ídem). |
| `valorations` | `Valoration` | Valoraciones recibidas por un usuario. **Entidad muerta** (sin uso real). |
| `storyMaps` | `StoryMap` | Historias anónimas geolocalizadas (mapa mundial). |
| `TimeMachine` | `TimeMachine` | "Carta al futuro": mensaje + email + fecha de entrega (la elige el usuario). |
| `bottles` | `Bottle` | "Botella al mar": mensaje anónimo con autor, marca de recibida y fecha. |
| `private_messages` | `PrivateMessage` | Mensajes 1:1 usuario ↔ profesional (chat privado, `/api/chats`). |
| `communities` | `Community` | Comunidades temáticas de apoyo (con moderador, nota fijada, chat abierto/cerrado). |
| `community_members` | `CommunityMember` | **Membresía** usuario↔comunidad. Único por `(userId, communityId)`. |
| `community_messages` | `CommunityMessage` | Mensajes de chat dentro de una comunidad. |
| `reports` | `Report` | Reportes de moderación de **historias, mensajes de comunidad y mensajes privados** (`target_type`). |
| `report_audit` | `ReportAudit` | Auditoría de cambios de estado de un reporte. La rellena el trigger `trg_reports_audit` (`db/06`). |
| `events` | `Event` | Eventos de la comunidad (con contador global "Me interesa"). |

## Relaciones

### Con integridad referencial (FK física en BD)
- **`users` N:1 `professions`** — `users.professionId → professions.professionId` (FK a entidad muerta)
- **`users` N:1 `specializations`** — `users.specializationId → specializations.specializationId` (FK a entidad muerta)
- **`valorations` N:1 `users`** — `valorations.userId → users.userId` (NOT NULL)
- **`reports` N:1 `storyMaps`** — `reports.story_id → storyMaps.id` (`@ManyToOne`, **nullable**: solo cuando `target_type = STORY`)
- **`reports` N:1 `users`** — `reports.resolved_by → users.userId` (`@ManyToOne`, moderador que resolvió; nullable)

### Relaciones lógicas (aún sin FK física)
Estas columnas referencian a otras tablas por `id` pero **no** declaran clave
foránea, por lo que el SGBD no garantiza la integridad:
- `community_members.userId` → `users.userId`; `community_members.communityId` → `communities.id` (con UNIQUE de pareja)
- `private_messages.userId` / `private_messages.professionalId` → `users.userId`
- `community_messages.communityId` → `communities.id`; `community_messages.userId` → `users.userId`
- `reports.message_id` → el mensaje reportado (de comunidad o privado, según `target_type`); `reports.reporter_id` → `users.userId`
- `report_audit.report_id` → `reports.id`; `report_audit.moderator_id` → `users.userId`

> **Mejora futura (opcional):** convertirlas en `@ManyToOne` con `@JoinColumn`
> añadiría integridad referencial. No se hace ahora para no interferir con el
> desarrollo activo de esas features ni con datos ya insertados.

## Restricciones de integridad

| Tipo | Dónde |
|---|---|
| **PK** | Todas las tablas (autoincremental, `GenerationType.IDENTITY`). |
| **UNIQUE** | `users.userName`, `users.nickName`, `users.mail`, `users.secretKey`; `professions.professionDescription`; `specializations.specializationDescription`; **`community_members (userId, communityId)`** (compuesta). |
| **NOT NULL** | `users.userName/nickName/userPassword/creationDate/role/twoFactorEnabled/warnings/banned`; `community_members.userId/communityId`; `bottles.received/created_at`; `reports.target_type/reason/status/created_at`; `valorations.*`; etc. |
| **FK** | Ver sección "Relaciones" (incluye `reports.story_id` y `reports.resolved_by`). |
| **Enum** | `users.role` (`UserRole`), `communities.category` (`CommunityTypes`), `reports.target_type` (`ReportTargetType`: STORY/MESSAGE/PRIVATE_MESSAGE), `reports.status` (`ReportStatus`: PENDING/RESOLVED/DISMISSED). |
| **Validación (capa app)** | `@Email` en `TimeMachine.email`; `@NotBlank` en `Bottle.message`. |

## Obtener el esquema real (DDL) desde la BD

El DDL exacto lo genera Hibernate. Para exportarlo como referencia/documentación:

```bash
docker compose -f ../.devcontainer/compose.yml exec mysql \
  mysqldump -u root -ppasahitza --no-data --skip-comments shareYourStory > db/schema-snapshot.sql
```

> Nota: las **tablas** las crea Hibernate al arrancar la app (`ddl-auto=update`). En una BD
> ya existente, `update` **no** relaja `NOT NULL` a nullable ni recrea constraints (p. ej. si
> `reports.story_id` pasó a nullable, hay que `ALTER` a mano).
