# Replicación y acceso considerando la réplica

Cumple los requisitos de Nivel 3: *"Desarrolla una replicación"* y *"gestiona el
acceso de forma eficiente, teniendo en cuenta la replicación"*.

## Arquitectura

```
                escrituras (app_rw)            lecturas (app_ro)
   Aplicación  ───────────────────►  PRIMARIO  ───────────────►  Aplicación
   (Spring)                          :3306                       (stats)
                                        │  replicación GTID
                                        ▼
                                     RÉPLICA  ◄───────────────  Aplicación
                                     :3307  (solo lectura)        (lecturas)
```

- **Primario** (`mysql-primary`, puerto 3306): recibe **escrituras y lecturas** de
  la aplicación (usuario `app_rw`).
- **Réplica** (`mysql-replica`, puerto 3307): copia de **solo lectura** (`read-only`),
  sincronizada por replicación **GTID**. La aplicación la usa para **consultas de
  solo lectura** (estadísticas/dashboard) con el usuario `app_ro`.

Tipo de replicación: **asíncrona, basada en GTID** (la más recomendada en MySQL 8,
sin tener que gestionar manualmente fichero/posición del binlog).

## Finalidad en este proyecto

- **Repartir carga:** las lecturas pesadas (estadísticas, panel) no compiten con
  las escrituras del primario.
- **Disponibilidad / copia caliente:** la réplica es una copia continua lista para
  promoverse si el primario cae.
- **Acceso eficiente:** el usuario `app_ro` (solo `SELECT`) sirve las lecturas; el
  primario queda reservado para `app_rw`.

## Puesta en marcha

```bash
# 1) Levantar primario + réplica
docker compose -f db/07-replicacion/docker-compose.yml up -d

# 2) Crear BD, usuario de replicación y usuarios de app EN EL PRIMARIO
docker exec -i syss-mysql-primary mysql -u root -ppasahitza \
  < db/07-replicacion/07-primary-users.sql

# 3) Iniciar la replicación EN LA RÉPLICA
docker exec -i syss-mysql-replica mysql -u root -ppasahitza \
  < db/07-replicacion/07-setup-replica.sql
```

En el paso 3 deben verse `Replica_IO_Running: Yes` y `Replica_SQL_Running: Yes`.

## Conectar la aplicación (reparto lectura/escritura)

Arrancar el backend apuntando el primario al 3306 y la réplica al 3307:

```bash
export DB_URL=jdbc:mysql://localhost:3306/shareYourStory
export DB_USERNAME=app_rw
export DB_PASSWORD=app_rw_pwd

export REPLICA_ENABLED=true
export REPLICA_DB_URL=jdbc:mysql://localhost:3307/shareYourStory
export REPLICA_DB_USERNAME=app_ro
export REPLICA_DB_PASSWORD=app_ro_pwd

./mvnw spring-boot:run
```

- Hibernate crea el esquema en el **primario** (app_rw); se replica a la réplica.
- `ReplicaDataSourceConfig` activa un **segundo DataSource de solo lectura**.
- `GET /api/moderation/stats` (`StatsController`) sirve sus contadores **desde la
  réplica** (`app_ro`), demostrando el reparto. Si `REPLICA_ENABLED=false`, el
  endpoint responde indicando que la réplica está deshabilitada.

## Verificación de la replicación

```bash
# Escribir en el primario
docker exec -i syss-mysql-primary mysql -u root -ppasahitza shareYourStory \
  -e "CREATE TABLE IF NOT EXISTS ping(id INT PRIMARY KEY AUTO_INCREMENT, t DATETIME);
      INSERT INTO ping(t) VALUES (NOW());"

# Leer en la réplica (debe aparecer la fila)
docker exec -i syss-mysql-replica mysql -u root -ppasahitza shareYourStory \
  -e "SELECT * FROM ping;"

# La réplica es de solo lectura: esto DEBE fallar
docker exec -i syss-mysql-replica mysql -u app_ro -papp_ro_pwd shareYourStory \
  -e "INSERT INTO ping(t) VALUES (NOW());"   # ERROR: --read-only
```

## Apagar

```bash
docker compose -f db/07-replicacion/docker-compose.yml down -v
```

## Problemas frecuentes

| Síntoma | Causa / solución |
|---|---|
| `Replica_IO_Running: Connecting` | La réplica no llega al primario o auth falla. Verificar que `repl` existe y `GET_SOURCE_PUBLIC_KEY=1` está puesto (MySQL 8 usa `caching_sha2_password`). |
| Error al crear funciones con binlog | Ejecutar en el primario: `SET GLOBAL log_bin_trust_function_creators = 1;` (ya viene como flag en el compose). |
| La réplica no tiene los datos antiguos | Con `SOURCE_AUTO_POSITION=1` se traen todas las transacciones GTID; si se desincroniza, `STOP REPLICA; RESET REPLICA ALL;` y repetir el paso 3. |
