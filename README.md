# ShareYourStory — Backend

API REST + WebSocket para **ShareYourStory**, una app web de apoyo emocional para jóvenes
(comunidades de chat moderadas, chat 1‑a‑1 con profesionales, eventos, botella al mar,
máquina del tiempo, mapa de historias y panel de moderación).

Es un proyecto **PBL** (Project‑Based Learning): el alcance está más cerca de "demo funcional"
que de producto en producción. El frontend (React) vive en el repo hermano
`ShareYourStory-PBL-frontend/`.

> Estado (2026‑06‑03): el backend está **implementado y compila**, pero **no se ha probado
> end‑to‑end en ejecución**. Ver "Pendientes" al final.

---

## Stack

- **Java 21** + **Spring Boot 4.1.0‑SNAPSHOT** (snapshot — build no reproducible, ver Pendientes).
  Repo de snapshots añadido en `pom.xml`; si Maven falla resolviendo dependencias, verificar
  acceso a `https://repo.spring.io/snapshot`.
- **spring-boot-starter-webmvc**, **-data-jpa**, **-validation**, **-security**, **-websocket**, **-mail**.
- **MySQL** (`mysql-connector-j`), `ddl-auto=update`, base de datos `shareYourStory`.
- **JWT** con `io.jsonwebtoken:jjwt` 0.12.5 (HS256).
- **2FA TOTP** con `com.warrenstrange:googleauth` 1.5.0 (Google Authenticator).
- **Rate limiting** con `com.bucket4j:bucket4j-core` 8.10.1 (ver [`RATE_LIMITING.md`](RATE_LIMITING.md)).
- **Sin Lombok** → getters/setters a mano. (Se quitaron del `pom.xml` `lombok` y
  `oauth2-authorization-server`, ambos sin uso.)
- Maven wrapper (`mvnw` / `mvnw.cmd`). Paquete raíz `shareyourstory`, clase main
  `ShareYourStoryApplication`.

---

## Arquitectura

Organización por dominios bajo `src/main/java/shareyourstory/`:

```
shareyourstory/
├── ShareYourStoryApplication.java
├── auth/                  # autenticación, JWT, 2FA, seguridad, rate limiting
│   ├── controller/        # AuthController, RateLimitService
│   ├── config/            # SecurityConfig, RateLimitFilter
│   ├── service/           # AuthService, GoogleAuthService, UserService
│   └── JWT/               # JWTService, AuthTokenFilter
├── config/                # GlobalExceptionHandler (@RestControllerAdvice), WebMvcConfig
├── websocket/             # WebSocketConfig, WebSocketService (STOMP)
└── domain/
    ├── user/              # User, UserRole, repositorios, UsersMeController, ProfessionalController
    ├── community/         # comunidades + membresía (community_members) + mensajes
    ├── bottle/            # botella al mar + chat privado (PrivateMessageController, /api/chats)
    ├── event/             # eventos + interés
    ├── storyMap/          # mapa de historias
    ├── timeMachine/       # cartas al yo futuro (scheduler + email)
    └── moderation/        # reportes, miembros, stats, procedimientos almacenados
```

---

## Cómo arrancar

### Con devcontainer (recomendado)

El `compose.yml` (en `../.devcontainer/`) orquesta `java-app`, `frontend` y `mysql`.
Abrir el workspace en VS Code → "Reopen in Container".

- Backend: `http://localhost:8080` · Frontend: `http://localhost:5173` · MySQL: `3306`.
- MySQL arranca con `--log-bin-trust-function-creators=1` (necesario para que `app_admin`
  pueda crear la función de `db/04` y el trigger de `db/06` con el binlog activo).
- En el **primer** arranque (volumen vacío) MySQL ejecuta `db/02-usuarios-permisos.sql`
  (crea `app_rw` / `app_ro` / `app_admin`). **`db/04` y `db/06` NO se ejecutan solos**
  (dependen de tablas que crea Hibernate al arrancar la app) → hay que correrlos a mano
  después del primer arranque. Ver [`db/README.md`](db/README.md).

### Solo backend (sin devcontainer)

```powershell
.\mvnw.cmd spring-boot:run
```

Necesita un MySQL en `localhost:3306` con la BD `shareYourStory` y el usuario `app_rw`
(o sobreescribir `DB_*` por entorno). Para levantar solo MySQL desde el compose:
`docker compose -f ../.devcontainer/compose.yml up mysql`.

---

## Configuración (variables de entorno)

Todas tienen valor por defecto **solo para desarrollo** en `application.properties`.
En producción hay que definirlas por entorno.

| Variable | Por defecto (dev) | Notas |
|---|---|---|
| `DB_URL` | `jdbc:mysql://mysql:3306/shareYourStory` | Datasource principal |
| `DB_USERNAME` | `app_rw` | **Mínimo privilegio**, NO `root` |
| `DB_PASSWORD` | `app_rw_pwd` | Solo dev |
| `JWT_SECRET` | (clave de dev) | **Definir en producción** |
| `JWT_EXPIRATION` | `3600000` (1 h) | Milisegundos |
| `MAIL_HOST` / `MAIL_PORT` | `smtp.gmail.com` / `587` | SMTP de la máquina del tiempo |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | (vacío) | App Password de Gmail — **nunca en el repo** |
| `REPLICA_ENABLED` | `false` | Replicación opcional (ver `db/07-replicacion`) |
| `REPLICA_DB_*` | localhost:3307 | Datasource réplica de solo lectura |

> ⚠️ **Seguridad:** una App Password de Gmail estuvo hardcodeada en `EmailConfig.java` y
> **sigue en el historial git**. Revocarla y usar `MAIL_USERNAME`/`MAIL_PASSWORD` por entorno.

---

## Seguridad

- **JWT** (`JWTService`, HS256, subject = username). `AuthTokenFilter` valida el `Bearer`,
  carga el `UserDetails` y puebla el `SecurityContext`; captura la firma inválida.
- **Roles** (`UserRole`): `ANON | USER | PROFESSIONAL | ADMINISTRATOR`. Spring expone
  `ROLE_<role>` como authority.
- **Autorización granular** en `SecurityConfig` (no es "todo permitAll"):
  - `/api/auth/register/mod/**` → `ADMINISTRATOR`; resto de `/api/auth/**` → público.
  - `/api/users/me/**`, `/api/professionals/**`, `/api/chats/**` → autenticado.
  - `GET /api/events*` → público; `POST/PUT/DELETE` de eventos y comunidades, y casi todo
    `/api/moderation/**` → `PROFESSIONAL` o `ADMINISTRATOR`. `POST /api/moderation/reports`
    e `interest` de eventos → autenticado.
- **2FA TOTP** obligatorio para staff (PROFESSIONAL/ADMINISTRATOR) en el login. Flujo real
  (no hay endpoints `/verify`):
  1. `GET /api/auth/register/mod/2fa/qr?email=` → URI `otpauth://` para el QR.
  2. `POST /api/auth/register/mod/2fa/qr {email,code}` → valida el primer código y marca
     `twoFactorEnabled=true`.
  3. `POST /api/auth/login/mod {email,password}` → `{challengeId, requires2fa}` (UUID con
     TTL 5 min, **en memoria**).
  4. `POST /api/auth/login/mod/2fa/code {challengeId,code}` → `{token}` (JWT).
- **Bootstrap del primer admin:** `POST /api/auth/register/admin/bootstrap` (201; 409 si ya
  existe alguno). A partir de ahí, los demás mods/admins se crean con `register/mod`
  (que exige ser ya ADMINISTRATOR).
- **Rate limiting** por IP y por token (429). Reglas y umbrales en [`RATE_LIMITING.md`](RATE_LIMITING.md).
- **CORS** abierto a `http://localhost:5173` con `GET/POST/PUT/PATCH/DELETE/OPTIONS`. CSRF off.
- **Errores** centralizados en `GlobalExceptionHandler` (`@RestControllerAdvice`):
  `401` (credenciales/no autenticado), `409` (integridad/duplicado), `404` (no encontrado),
  `400` (validación). Sin catch‑all a propósito (para no pisar los status nativos de Spring).

---

## Inventario de endpoints (47)

**Auth** (`/api/auth`, salvo donde se indique)
`POST /anonymous` · `POST /register` · `POST /login` · `POST /register/mod` ·
`POST /register/admin/bootstrap` · `GET+POST /register/mod/2fa/qr` · `POST /login/mod` ·
`POST /login/mod/2fa/code` · `GET /api/users/me` · `PATCH /api/users/me/username` · `GET /api/testJWT`

**users/me** (`/api/users/me`)
`GET /profile` · `GET+PATCH /mod-profile` · `PATCH /password` · `POST /onboarding` ·
`PATCH /settings` · `POST /mood` · `GET /dashboard/messages`
> `settings` y `mood` se aceptan pero **no persisten**; `profile.activity` se entrega vacío.

**Communities** (`/api/communities`)
`GET /` · `POST /` · `PUT /{id}` · `DELETE /{id}` · `POST+DELETE /{id}/join` ·
`GET /{id}/members/active` · `DELETE /{id}/members/{userId}` · `POST /{id}/online` ·
`PATCH /{id}/pinned-note` · `PATCH /{id}/chat-closed` · `GET+POST /{id}/messages` ·
`DELETE /{id}/messages/{messageId}`

**Chat privado** (`/api/chats`) — identidad por JWT, sin `userId` del cliente
`GET+POST /{otherId}/messages` · `GET /inbox` · `GET+POST /inbox/{userId}/messages`

**Events**
`GET /api/events` · `POST /api/events` · `GET /api/events/{id}` ·
`POST+DELETE /api/events/{id}/interest` · `PUT /api/events/{id}` · `DELETE /api/events/{id}`

**Bottles** · `POST /api/bottles` · `GET /api/bottles/received` · `GET /api/bottles/floating`
**Stories** · `GET+POST /api/stories`
**Professionals** · `GET /api/professionals`
**TimeMachine** · `POST /api/timeMachine` (el usuario elige `deliveryDate`)

**Moderation** (`/api/moderation`)
`POST+GET /reports` · `GET /reports/pending` · `POST /reports/{id}/resolve {action: resolve|warn|dismiss}` ·
`GET /reports/{id}/audit` · `GET /members` · `POST /members/{id}/warn` · `POST /members/{id}/ban` · `GET /stats`

---

## WebSocket (tiempo real)

STOMP con broker simple en `/topic`. Difusión por `/topic/storyMap`, `/topic/events`,
`/topic/communities` y `/topic/communities/{id}`. Los **mensajes privados van por la cola de
usuario `/user/queue/private`** (`convertAndSendToUser`), no por un topic compartido. El
CONNECT se autentica con JWT.

---

## Base de datos

La carpeta `db/` documenta el modelo y la administración (ver [`db/README.md`](db/README.md)):

| Paso | Contenido | ¿Se ejecuta solo? |
|---|---|---|
| `db/01-modelo-er.md` | Modelo entidad‑relación | — (doc) |
| `db/02-usuarios-permisos.sql` | Crea `app_rw` / `app_ro` / `app_admin` | **Sí**, en el primer arranque del compose |
| `db/03-*` | Backups (script + doc) | manual |
| `db/04-procedimientos.sql` | `sp_resolve_report`, `fn_count_pending_reports` | **No** — correr a mano tras el primer arranque |
| `db/05-transacciones.md` | Transacciones | — (doc) |
| `db/06-triggers.sql` | `trg_reports_audit` | **No** — correr a mano tras el primer arranque |
| `db/07-replicacion/` | Réplica de solo lectura (opcional) | manual, desactivado por defecto |

- La app conecta como **`app_rw`** (mínimo privilegio), no `root`.
- `ddl-auto=update` **no** relaja `NOT NULL` ni recrea constraints en una BD ya existente:
  para columnas que pasaron a nullable (p. ej. `reports.story_id`) hay que `ALTER` a mano.
- Los procedimientos/función/trigger de moderación son **agnósticos al objetivo del reporte**
  (historia / mensaje de comunidad / mensaje privado) y se siguen usando sin cambios.

---

## Tests

```powershell
.\mvnw.cmd test
```

Hoy solo hay `contextLoads` (no corre sin una BD disponible). Falta cobertura de contrato
(MockMvc por dominio). Ver Pendientes.

---

## Pendientes

La lista activa y priorizada (incluye cosas fuera del código) está en el **Roadmap** del
frontend: `../ShareYourStory-PBL-frontend/CLAUDE.md` → sección "Pendientes / Roadmap".
En resumen, lo más urgente del lado backend:

1. **Revocar la App Password de Gmail** comprometida (sigue en el historial git).
2. **Probar end‑to‑end** (login/2FA, comunidades, chat privado en tiempo real, eventos,
   moderación, botella, máquina del tiempo). Solo compila.
3. Fijar una **versión release** de Spring Boot (hoy `4.1.0‑SNAPSHOT`).
4. Limpiar entidades muertas (`Valoration`, `Profession`, `Specialization` referenciadas por
   FK en `User`), `WebMvcConfig` vacío y el comentario obsoleto de `WebSocketConfig`.
5. Mover challenges de 2FA y buckets de rate limit a **Redis/tabla** para producción
   (hoy en memoria, se pierden al reiniciar y no se comparten entre instancias).
