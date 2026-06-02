# Modelo Entidad-Relación — ShareYourStory

Modelo de datos real generado por Hibernate a partir de las entidades JPA
(`src/main/java/shareyourstory/domain/**/model`).

## Diagrama E/R

```mermaid
erDiagram
    PROFESSIONS      ||--o{ USERS              : "clasifica"
    SPECIALIZATIONS  ||--o{ USERS              : "clasifica"
    USERS            ||--o{ VALORATIONS        : "recibe"
    USERS            ||..o{ PRIVATE_MESSAGES   : "participa (lógica, sin FK)"
    COMMUNITIES      ||..o{ COMMUNITY_MESSAGES : "contiene (lógica, sin FK)"
    USERS            ||..o{ COMMUNITY_MESSAGES : "escribe (lógica, sin FK)"

    USERS {
        int     userId PK
        string  userName  UK "NOT NULL"
        string  nickName  UK "NOT NULL"
        string  mail      UK
        string  userPassword "NOT NULL (BCrypt)"
        string  secretKey UK "TOTP 2FA"
        enum    role "ANON|USER|PROFESSIONAL|ADMINISTRATOR"
        date    creationDate "NOT NULL"
        int     professionId FK
        int     specializationId FK
        bool    twoFactorEnabled "NOT NULL"
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
        string  from "user|professional"
    }
    COMMUNITIES {
        bigint  id PK
        string  name
        enum    category
        int     members
        int     online
    }
    COMMUNITY_MESSAGES {
        bigint  id PK
        int     communityId "lógica → COMMUNITIES"
        int     userId "lógica → USERS"
        string  username
        string  text
        datetime createdAt
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
        string  email
        date    deliveryDate
    }
    BOTTLES {
        int     id PK
        string  message "NOT BLANK"
    }
    EVENTS {
        int     id PK
        string  title
        string  date
        string  place
    }
```

## Entidades

| Tabla | Entidad JPA | Descripción |
|---|---|---|
| `users` | `User` | Cuentas (anónimas, usuarios, profesionales, administradores). Implementa `UserDetails`. |
| `professions` | `Profession` | Catálogo de profesiones de los profesionales. |
| `specializations` | `Specialization` | Catálogo de especializaciones. |
| `valorations` | `Valoration` | Valoraciones recibidas por un usuario/profesional. |
| `storyMaps` | `StoryMap` | Historias anónimas geolocalizadas (mapa mundial). |
| `TimeMachine` | `TimeMachine` | "Carta al futuro": mensaje + email + fecha de entrega. |
| `bottles` | `Bottle` | "Botella flotante": mensaje anónimo. |
| `private_messages` | `PrivateMessage` | Mensajes 1:1 usuario ↔ profesional. |
| `communities` | `Community` | Comunidades temáticas de apoyo. |
| `community_messages` | `CommunityMessage` | Mensajes de chat dentro de una comunidad. |
| `events` | `Event` | Eventos de la comunidad. |

## Relaciones

### Con integridad referencial (FK física en BD)
- **`users` N:1 `professions`** — `users.professionId → professions.professionId`
- **`users` N:1 `specializations`** — `users.specializationId → specializations.specializationId`
- **`valorations` N:1 `users`** — `valorations.userId → users.userId` (NOT NULL)

### Relaciones lógicas (aún sin FK física)
Estas columnas referencian a otras tablas por `id` pero **no** declaran clave
foránea, por lo que el SGBD no garantiza la integridad:
- `private_messages.userId` / `private_messages.professionalId` → `users.userId`
- `community_messages.communityId` → `communities.id`
- `community_messages.userId` → `users.userId`

> **Mejora futura (opcional):** convertirlas en `@ManyToOne` con `@JoinColumn`
> añadiría integridad referencial. No se hace ahora para no interferir con el
> desarrollo activo de esas features ni con datos ya insertados.

## Restricciones de integridad

| Tipo | Dónde |
|---|---|
| **PK** | Todas las tablas (autoincremental, `GenerationType.IDENTITY`). |
| **UNIQUE** | `users.userName`, `users.nickName`, `users.mail`, `users.secretKey`; `professions.professionDescription`; `specializations.specializationDescription`. |
| **NOT NULL** | `users.userName/nickName/userPassword/creationDate/role/twoFactorEnabled`; `valorations.*`; etc. |
| **FK** | Ver sección "Relaciones". |
| **Enum** | `users.role` (`UserRole`), `communities.category` (`CommunityTypes`). |
| **Validación (capa app)** | `@Email` en `TimeMachine.email`; `@NotBlank` en `Bottle.message`. |

## Obtener el esquema real (DDL) desde la BD

El DDL exacto lo genera Hibernate. Para exportarlo como referencia/documentación:

```bash
docker compose -f .devcontainer/compose.yml exec mysql \
  mysqldump -u root -ppasahitza --no-data --skip-comments shareYourStory > db/schema-snapshot.sql
```
