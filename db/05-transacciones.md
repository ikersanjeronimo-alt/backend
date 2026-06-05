# Transacciones

Cumple el requisito *"Utiliza transacciones y argumenta su uso si procede"*
(Nivel 2).

## Dónde

`ModerationService.resolveReport(...)` (anotado con `@Transactional`).

## Qué operación es transaccional

El endpoint recibe `action: "resolve" | "warn" | "dismiss"`. Internamente:
`dismiss` → estado `DISMISSED` (solo toca `reports`); `resolve` y `warn` → estado `RESOLVED`
y **además sanean el contenido infractor**; `warn` incrementa también los avisos del autor.

Por eso, resolver (`resolve`/`warn`) modifica **varias tablas en una sola unidad de trabajo**:

1. **`reports`** — mediante el procedimiento almacenado `sp_resolve_report`
   (status → `RESOLVED`/`DISMISSED`, `resolved_by`, `resolved_at`).
2. **El contenido infractor**, según `target_type` del reporte:
   - `STORY` → se **borra** la fila de `storyMaps` por completo (antes se desligan
     los reportes que la referencian poniendo `reports.story_id = NULL`, para no
     violar la FK; el snapshot `reports.content` conserva el texto reportado). Se
     difunde un evento `DELETE` por `/topic/storyMap` para que el mapa lo quite en vivo.
   - `MESSAGE` → se **borra** la fila de `community_messages` (igual que el delete del
     moderador) y se difunde un evento `DELETE` por `/topic/communities/{id}` para que
     desaparezca en vivo en todas las sesiones del chat. El snapshot `reports.content`
     conserva el texto reportado para el panel.
   - `PRIVATE_MESSAGE` → se **borra** la fila de `private_messages` y se difunde un
     evento `DELETE` a la cola personal de ambos participantes (`/user/queue/private`)
     para que desaparezca en vivo en el chat. El snapshot `reports.content` conserva
     el texto reportado.
3. **`users`** (solo `warn`) — incrementa `warnings` del autor reportado.

```java
@Transactional
public void resolveReport(Integer reportId, Integer moderatorId, String action) {
    String act = action.trim().toLowerCase();                 // resolve | warn | dismiss
    String spAction = act.equals("dismiss") ? "DISMISSED" : "RESOLVED";
    reportRepository.resolveReport(reportId, moderatorId, spAction);   // CALL sp_resolve_report → tabla reports
    if (!spAction.equals("RESOLVED")) return;

    Report report = reportRepository.findById(reportId).orElseThrow(...);
    // sanea historia / mensaje de comunidad / mensaje privado según target_type
    // ...
    if (act.equals("warn")) { /* users.warnings++ del autor reportado */ }
}
```

## Argumento (¿por qué procede una transacción?)

Las escrituras forman **una sola unidad de trabajo**: o se confirman todas o
ninguna (propiedad de **atomicidad**, la "A" de ACID).

- Si se confirmara el paso 1 pero fallara el saneado, quedaría un reporte marcado
  como **RESUELTO** sobre un contenido que **sigue mostrando** el material
  inapropiado: un estado incoherente.
- Con `@Transactional`, ante cualquier `RuntimeException` Spring hace **rollback**
  de toda la operación, incluida la ejecución del procedimiento almacenado (la
  llamada `CALL` participa en la misma transacción JDBC, sin autocommit).

## Cómo demostrar el rollback (para la defensa)

1. Forzar un fallo en el paso 2 temporalmente, p. ej. lanzando una excepción
   justo después de llamar al procedimiento:
   ```java
   reportRepository.resolveReport(reportId, moderatorId, action);
   if (true) throw new RuntimeException("fallo simulado");
   ```
2. Llamar a `POST /api/moderation/reports/{id}/resolve` con `action=resolve`.
3. Comprobar que **el reporte sigue en `PENDING`** (el procedimiento se revirtió):
   ```sql
   SELECT id, status, resolved_by FROM reports WHERE id = <id>;
   ```
4. Quitar la línea de fallo simulado.

> Nota: las operaciones de una sola tabla (crear reporte, listar) ya son atómicas
> por sí mismas; ahí una transacción explícita no aporta y por eso **no** se añade
> ("si procede").
