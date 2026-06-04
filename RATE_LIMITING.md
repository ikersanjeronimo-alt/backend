# Rate limiting

Limitación de frecuencia de los endpoints sensibles del backend (login/2FA, registro, envío
de correo y escritura de contenido) para mitigar fuerza bruta y abuso. Implementado con
**bucket4j** (token‑bucket en memoria).

> Este documento consolida lo que antes estaba repartido (y vacío) entre
> `RATE_LIMITING.md`, `IMPLEMENTACION_RATE_LIMITING.md` y `RESUMEN_RATE_LIMITING_ES.md`.
> Los dos últimos se han eliminado: esta es la fuente única.

---

## Cómo funciona

Dos piezas, ambas en `src/main/java/shareyourstory/auth/`:

- **`RateLimitFilter`** (`config/RateLimitFilter.java`) — un `OncePerRequestFilter` que, para
  cada petición, mira si la combinación método + ruta cae bajo alguna regla. Si la hay:
  1. Consume 1 token del cubo **por IP** (`<regla>:ip:<ip>`).
  2. Si la petición trae `Authorization: Bearer <token>`, consume además 1 token del cubo
     **por token** (`<regla>:tok:<token>`).
  3. Se rechaza con **`429 Too Many Requests`** en cuanto **cualquiera** de los dos cubos se
     agota. El cuerpo es JSON: `{"error":"Demasiadas peticiones. Intentalo de nuevo en un minuto."}`.
  - La IP del cliente se toma de `X-Forwarded-For` (primer valor) si está presente, si no de
    `getRemoteAddr()`.
- **`RateLimitService`** (`controller/RateLimitService.java`) — mantiene un
  `ConcurrentHashMap<String, Bucket>` y crea cada cubo bajo demanda con
  `Bandwidth.classic(capacidad, Refill.greedy(capacidad, periodo))`. La recarga es **greedy**
  (continua dentro de la ventana), no a pulsos. La clave del cubo incluye capacidad y periodo
  (`key|capacity|periodMillis`) para que dos reglas distintas sobre la misma clave no colisionen.

Doble cubo (IP **y** token) sirve para dos amenazas distintas: la IP frena a un atacante sin
sesión; el token frena a un usuario autenticado que abusa aunque rote de IP.

---

## Reglas y umbrales

Solo se limitan peticiones **POST**. Definidas en `RateLimitFilter.ruleFor(...)`:

| Regla | Rutas (POST) | Límite |
|---|---|---|
| `2fa` | `/api/auth/login/mod/2fa/code` | **5 / min** |
| `login` | `/api/auth/login`, `/api/auth/login/mod` | **10 / min** |
| `register` | `/api/auth/register`, `/api/auth/anonymous` | **20 / min** |
| `mail` | `/api/timeMachine`, `/api/letters` | **5 / min** |
| `content` | `/api/bottles`, `/api/stories`, y cualquier ruta que termine en `/messages` | **30 / min** |

> Las rutas `…/messages` cubren tanto los mensajes de comunidad (`/api/communities/{id}/messages`)
> como los del chat privado (`/api/chats/...`). `GET`, `PUT`, `PATCH` y `DELETE` no se limitan.

Cada límite es **por IP** y, adicionalmente, **por token** si la petición está autenticada.

---

## Probarlo

Con el backend levantado, lanzar más peticiones que el umbral dentro de un minuto:

```bash
# 12 logins seguidos (umbral 10/min) -> las 2 últimas deberían dar 429
for i in $(seq 1 12); do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"x","password":"y"}'
done
```

---

## Limitaciones y pendientes

- **En memoria**: los cubos viven en el proceso. Se pierden al reiniciar y **no se comparten
  entre instancias** → en un despliegue multi‑instancia el límite efectivo se multiplica por el
  número de nodos. Para producción, mover a un backend distribuido (bucket4j soporta Redis).
- Las reglas y umbrales están **hardcodeados** en `RateLimitFilter`. Si se necesitan ajustar
  con frecuencia, externalizarlos a `application.properties`.
- No hay cabeceras `Retry-After` ni `X-RateLimit-*` en la respuesta 429 (mejora opcional).
