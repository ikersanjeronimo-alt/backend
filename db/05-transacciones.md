# Transacciones

Cumple el requisito *"Utiliza transacciones y argumenta su uso si procede"*
(Nivel 2).

## Dónde

`ModerationService.resolveReport(...)` (anotado con `@Transactional`).

## Qué operación es transaccional

Resolver un reporte con acción `RESOLVED` modifica **dos tablas**:

1. **`reports`** — mediante el procedimiento almacenado `sp_resolve_report`
   (status → `RESOLVED`, `resolved_by`, `resolved_at`).
2. **`storyMaps`** — se sanea el mensaje de la historia infractora
   (`"[eliminado por moderación]"`).

```java
@Transactional
public void resolveReport(Integer reportId, Integer moderatorId, String action) {
    reportRepository.resolveReport(reportId, moderatorId, action);   // tabla reports
    if ("RESOLVED".equalsIgnoreCase(action)) {
        Report report = reportRepository.findById(reportId).orElseThrow(...);
        StoryMap story = report.getStory();
        story.setMessage("[eliminado por moderación]");              // tabla storyMaps
        storyMapRepository.save(story);
    }
}
```

## Argumento (¿por qué procede una transacción?)

Las dos escrituras forman **una sola unidad de trabajo**: o se confirman ambas o
ninguna (propiedad de **atomicidad**, la "A" de ACID).

- Si se confirmara el paso 1 pero fallara el paso 2, quedaría un reporte marcado
  como **RESUELTO** sobre una historia que **sigue mostrando** el contenido
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
2. Llamar a `POST /api/moderation/reports/{id}/resolve` con `action=RESOLVED`.
3. Comprobar que **el reporte sigue en `PENDING`** (el procedimiento se revirtió):
   ```sql
   SELECT id, status, resolved_by FROM reports WHERE id = <id>;
   ```
4. Quitar la línea de fallo simulado.

> Nota: las operaciones de una sola tabla (crear reporte, listar) ya son atómicas
> por sí mismas; ahí una transacción explícita no aporta y por eso **no** se añade
> ("si procede").
