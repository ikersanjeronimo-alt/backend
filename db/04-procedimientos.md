# Procedimientos y funciones almacenadas

Cumple el requisito *"Utiliza procedimientos almacenados para comunicarse con la
aplicación (PL/SQL)"* (Nivel 2). En MySQL el lenguaje es SQL/PSM (equivalente a
PL/SQL de Oracle).

## Qué se ha creado (`db/04-procedimientos.sql`)

| Objeto | Tipo | Qué hace |
|---|---|---|
| `sp_resolve_report(p_report_id, p_moderator_id, p_action)` | PROCEDIMIENTO | Marca un reporte como `RESOLVED`/`DISMISSED`, registra el moderador y la fecha. Valida la acción y que el reporte esté `PENDING` (lanza `SIGNAL` si no). |
| `fn_count_pending_reports()` | FUNCIÓN | Devuelve cuántos reportes están `PENDING`. |

## Cómo lo usa la aplicación (DAO → procedimiento)

- **Procedimiento:** `ReportRepositoryImpl.resolveReport(...)` lo invoca con
  `EntityManager.createStoredProcedureQuery("sp_resolve_report")` y parámetros
  `IN` posicionales. Es el patrón **DAO/Repository llamando a un procedimiento**.
- **Función:** `ReportRepository.countPendingReportsViaFunction()` la llama con
  `@Query(value = "SELECT fn_count_pending_reports()", nativeQuery = true)`.

## Endpoints REST (vertical de moderación)

| Método | Ruta | Acceso | Acción de BD |
|---|---|---|---|
| `POST` | `/api/moderation/reports` | Autenticado | `INSERT` (JPA) |
| `GET` | `/api/moderation/reports/pending` | `ADMINISTRATOR` | `SELECT` + `fn_count_pending_reports()` |
| `POST` | `/api/moderation/reports/{id}/resolve` | `ADMINISTRATOR` | `CALL sp_resolve_report(...)` |

El control de acceso se ha añadido en `SecurityConfig.java`.

## Orden de puesta en marcha

1. **Arrancar la app una vez** para que Hibernate cree la tabla `reports`
   (`ddl-auto=update`).
2. **Aplicar el script** de procedimientos:
   ```bash
   docker compose -f .devcontainer/compose.yml exec mysql \
     mysql -u app_admin -papp_admin_pwd shareYourStory < db/04-procedimientos.sql
   ```
3. Ya se pueden usar los endpoints `/api/moderation/**`.

> Si al crear la función diera error por el log binario (replicación, Paso 7),
> ejecutar como root: `SET GLOBAL log_bin_trust_function_creators = 1;`

## Prueba rápida

```bash
# 1) Crear una historia y reportarla (POST /api/storyMap, luego POST /api/moderation/reports)
# 2) Ver pendientes
curl -H "Authorization: Bearer <token-admin>" \
  http://localhost:8080/api/moderation/reports/pending
# 3) Resolver
curl -X POST -H "Authorization: Bearer <token-admin>" -H "Content-Type: application/json" \
  -d '{"action":"RESOLVED"}' \
  http://localhost:8080/api/moderation/reports/1/resolve
```
