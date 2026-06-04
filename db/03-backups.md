# Copias de seguridad y restauración

Cumple los requisitos *"realiza copias"* (Nivel 1) y *"desarrolla algún proceso
de restauración"* (Nivel 2).

## Herramienta

Se usa **`mysqldump`** (lógico, portable y suficiente para el tamaño del
proyecto), con el usuario **`app_admin`** (privilegios acotados a la BD, ver
`db/02-usuarios-permisos.md`). El volcado incluye **esquema + datos +
procedimientos + triggers + eventos** (`--routines --triggers --events`), por lo
que una restauración deja la BD completamente operativa.

## Scripts

| Script | Función |
|---|---|
| `03-backup.sh` | Crea `db/backups/shareYourStory_<fecha>.sql`. |
| `03-restore.sh <fichero>` | Restaura la BD desde un volcado. |

Los volcados se guardan en `db/backups/` y **no se versionan** (`.gitignore`),
porque pueden contener datos personales.

## Uso

### Opción A — Linux / devcontainer (cliente mysql disponible)
```bash
chmod +x db/03-backup.sh db/03-restore.sh
bash db/03-backup.sh
bash db/03-restore.sh db/backups/shareYourStory_20260602_010000.sql
```

### Opción B — Windows / host con Docker (PowerShell)
```powershell
# Backup
docker compose -f ../.devcontainer/compose.yml exec mysql `
  sh -c "mysqldump -u app_admin -papp_admin_pwd --single-transaction --routines --triggers --events --databases shareYourStory" `
  > db/backups/shareYourStory_manual.sql

# Restauración
Get-Content db/backups/shareYourStory_manual.sql | `
  docker compose -f ../.devcontainer/compose.yml exec -T mysql `
  mysql -u app_admin -papp_admin_pwd
```

## Frecuencia recomendada

| Entorno | Frecuencia | Retención |
|---|---|---|
| Desarrollo | Manual antes de cambios grandes de esquema | Última copia |
| Producción | **Diaria** (cron, p. ej. 03:00) | 7 diarias + 4 semanales |

Ejemplo de automatización con cron (producción):
```cron
0 3 * * * /ruta/al/proyecto/db/03-backup.sh >> /var/log/syss-backup.log 2>&1
```

## Procedimiento de restauración probado (evidencia para la defensa)

1. **Crear un backup** del estado actual:
   ```bash
   bash db/03-backup.sh
   ```
2. **Simular una pérdida** de datos (en un entorno de prueba):
   ```sql
   DROP TABLE valorations;        -- o un DELETE accidental
   ```
3. **Restaurar** desde el último volcado:
   ```bash
   bash db/03-restore.sh db/backups/shareYourStory_<fecha>.sql
   ```
4. **Verificar** que la tabla y los datos han vuelto:
   ```sql
   SELECT COUNT(*) FROM valorations;
   ```

> Recomendación: ejecutar estos 4 pasos antes de la entrega/defensa y guardar una
> captura de la salida como evidencia de que la restauración funciona.
