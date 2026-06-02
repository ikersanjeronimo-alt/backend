# Triggers (ejecución automática)

Cumple el requisito *"Utiliza procedimientos de ejecución automática (triggers)
si procede"* (Nivel 3).

## Qué se ha creado (`db/06-triggers.sql`)

| Trigger | Evento | Qué hace |
|---|---|---|
| `trg_reports_audit` | `AFTER UPDATE ON reports` | Si cambia `status`, inserta una fila en `report_audit` (estado anterior, nuevo, moderador y fecha). |

La tabla de auditoría `report_audit` está modelada como entidad JPA
(`ReportAudit`), por lo que Hibernate la crea automáticamente.

## Por qué un trigger (¿por qué "procede"?)

La auditoría debe ser **infalsificable y completa**: tiene que registrarse
**cualquier** cambio de estado de un reporte, sin depender de que el programador
se acuerde de escribir el log en cada sitio.

- Si la auditoría se hiciera en código Java, un cambio hecho por otra vía (un
  `UPDATE` manual de mantenimiento, otro servicio, una migración) **no quedaría
  registrado**.
- El trigger vive en el SGBD y se ejecuta **junto a la operación** (misma
  transacción): garantiza la traza pase lo que pase. Es el caso de uso canónico
  de un trigger de auditoría.

> Separación de responsabilidades: el **procedimiento** `sp_resolve_report`
> *realiza* el cambio; el **trigger** *lo audita*. No se solapan ni duplican.

## Consulta del historial

`GET /api/moderation/reports/{id}/audit` (solo `ADMINISTRATOR`) →
`ModerationService.auditFor(id)` → `report_audit`.

## Orden de puesta en marcha

1. Arrancar la app una vez (Hibernate crea `reports` y `report_audit`).
2. Aplicar procedimientos y trigger:
   ```bash
   docker compose -f .devcontainer/compose.yml exec mysql \
     mysql -u app_admin -papp_admin_pwd shareYourStory < db/04-procedimientos.sql
   docker compose -f .devcontainer/compose.yml exec mysql \
     mysql -u app_admin -papp_admin_pwd shareYourStory < db/06-triggers.sql
   ```

## Prueba (para la defensa)

1. Reportar una historia (`POST /api/moderation/reports`).
2. Resolverla (`POST /api/moderation/reports/{id}/resolve`, `action=RESOLVED`).
3. Comprobar que el trigger generó la fila de auditoría **sin** que el código la
   insertara:
   ```sql
   SELECT * FROM report_audit ORDER BY created_at DESC;
   ```
