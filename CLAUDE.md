# CLAUDE.md — Backend de ShareYourStory-PBL

> Contexto persistente para Claude en el repo del **backend**. No apuntes secretos aquí.
> El contexto de producto y el detalle de contrato front↔back vive en el `CLAUDE.md` del
> repo hermano `../ShareYourStory-PBL-frontend/CLAUDE.md` (más completo). Aquí va lo del backend.

---

## Reglas para Claude (idénticas al frontend)

- **Idioma:** responde siempre en castellano.
- **Preguntas:** al usuario le gusta que le pregunten. Ante ambigüedad de alcance, naming o diseño, **pregunta antes de implementar**.
- **Al terminar una tarea, SIEMPRE:** (1) resumen claro de qué cambió por ficheros/áreas (sin pegar el diff entero); (2) **comandos de commit listos para copiar**, formato simple en dos líneas (`git add <paths>` + `git commit -m "<prefijo>: <descripción corta>"`), prefijos `feat:`/`fix:`/`refactor:`/`docs:`/`style:`, sin HEREDOC ni cuerpos multilínea. **No commitear automáticamente** — solo dar los comandos.
- **Mantener este fichero vivo:** ante cambios relevantes (nuevo endpoint, refactor, cambio de stack) o decisiones importantes, actualízalo.
- **No subir secretos.**

---

## Qué es

API REST + WebSocket de **ShareYourStory**, app web de apoyo emocional para jóvenes
(comunidades moderadas, chat 1-a-1 con profesionales, eventos, botella al mar, máquina del
tiempo, mapa de historias, panel de moderación). Es un **PBL**: alcance "demo funcional".

**Estado (2026-06):** implementado y **compila**, pero **no probado end-to-end en ejecución**.

## Stack

- **Java 21** + **Spring Boot 4.1.0-SNAPSHOT** (snapshot → build no reproducible; repo de snapshots en `pom.xml`).
- Starters: `webmvc`, `data-jpa`, `validation`, `security`, `websocket` (STOMP), `mail`.
- **JWT** `io.jsonwebtoken:jjwt` 0.12.5 (HS256). **2FA TOTP** `com.warrenstrange:googleauth`. **Rate limiting** `com.bucket4j:bucket4j-core` (ver `RATE_LIMITING.md`).
- **MySQL** (`mysql-connector-j`), `ddl-auto=update`, BD `shareYourStory`.
- **Sin Lombok** → getters/setters a mano. Paquete raíz `shareyourstory`, main `ShareYourStoryApplication`.
- Maven wrapper (`mvnw` / `mvnw.cmd`).

## Estructura

- `src/main/java/shareyourstory/` → `auth/` (JWT, SecurityConfig, RateLimit, GoogleAuth), `config/` (GlobalExceptionHandler `@RestControllerAdvice`, WebMvcConfig), `websocket/` (STOMP), `domain/` (community, event, bottle [incl. chat privado], storyMap, timeMachine, moderation, user).
- `src/main/resources/application.properties` → todo externalizado por variables de entorno con defaults de dev.
- `db/` → 01 modelo ER, 02 usuarios/permisos, 03 backups, 04 procedimientos, 05 transacciones, 06 triggers, 07 replicación. Ver `db/README.md`.
- 47 endpoints en 11 controladores. Inventario detallado y cruce con el front: en el `CLAUDE.md` del frontend.

## Cómo arrancar

### Con devcontainer (recomendado)
`compose.yml` en `../.devcontainer/` orquesta `java-app` + `mysql`. VS Code → "Reopen in Container", luego dentro del contenedor:
```bash
cd /workspace/backend && ./mvnw spring-boot:run    # backend en :8080 (Hibernate crea tablas)
```
**Solo el primer arranque** (tras crear las tablas), cargar a mano los objetos que dependen de ellas:
```bash
mysql -h mysql -u root -ppasahitza shareYourStory < db/04-procedimientos.sql
mysql -h mysql -u root -ppasahitza shareYourStory < db/06-triggers.sql
```
(MySQL crea `app_rw`/`app_ro`/`app_admin` solo en el primer arranque vía `db/02`. La app se conecta como `app_rw`.)

### Solo backend (sin devcontainer)
```powershell
.\mvnw.cmd spring-boot:run
```
Necesita MySQL en `localhost:3306` con la BD `shareYourStory` (o sobreescribir `DB_*`). Solo MySQL desde compose: `docker compose -f ../.devcontainer/compose.yml up mysql`.

### Tests
```powershell
.\mvnw.cmd test
```

## Variables de entorno (defaults solo de dev en `application.properties`)

| Variable | Default dev | Notas |
|---|---|---|
| `DB_URL` | `jdbc:mysql://mysql:3306/shareYourStory` | Datasource principal |
| `DB_USERNAME` / `DB_PASSWORD` | `app_rw` / `app_rw_pwd` | Mínimo privilegio, NO root |
| `JWT_SECRET` / `JWT_EXPIRATION` | (clave dev) / `3600000` | Definir `JWT_SECRET` en prod |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | (vacío) | App Password Gmail por entorno |
| `REPLICA_ENABLED` | `false` | Replicación opcional (`db/07`) |

> ⚠️ **Seguridad pendiente:** una App Password de Gmail estuvo hardcodeada en `EmailConfig.java` y **sigue en el historial git**. Revocarla. Buen candidato para una pasada de `/cso`.

---

## gstack (equipo de IA por slash commands)

Este repo usa **gstack** cuando se abre Claude Code **desde WSL** (skills en `~/.claude/skills/gstack` del Ubuntu; en Windows nativo no están disponibles).

- **Navegación web:** usa `/browse` de gstack. **No** uses `mcp__claude-in-chrome__*`.
- **Skills:** `/office-hours`, `/plan-ceo-review`, `/plan-eng-review`, `/plan-design-review`, `/review`, `/cso`, `/investigate`, `/qa`, `/qa-only`, `/ship`, `/land-and-deploy`, `/canary`, `/browse`, `/autoplan`, `/learn`, `/gstack-upgrade`.
- **Para este backend:** `/review` (bugs de producción) y `/cso` (OWASP + STRIDE, incl. el secreto filtrado) son los de mayor valor; no requieren levantar la app. `/investigate` para depurar fallos de arranque/runtime.
- **gstack NO anula las reglas de arriba** (castellano, preguntar, dar comandos de commit sin commitear solo).
