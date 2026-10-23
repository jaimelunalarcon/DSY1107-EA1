# DSY1107 — Presupuestos (EA1 + EP2 RabbitMQ)

## 1. Objetivo

El sistema gestiona **solicitudes de presupuesto** (trabajador crea/edita; administrador decide) con Cognito PKCE, API Gateway y backend Spring Boot en ECS. En EP2 se agrega mensajería asíncrona con **RabbitMQ**: al crear o decidir una solicitud el backend publica eventos; `notificaciones-ms` los consume (notificación, ticket y auditor DLQ). Un microservicio aparte (`rabbit-admin-ms`) administra topología por REST. El contrato HTTP de `/presupuestos` y Cognito **no cambian**.

Repositorio: https://github.com/jaimelunalarcon/DSY1107-EA1

## 2. Arquitectura

```text
React (Amplify)
  |
  +--> Cognito OAuth2 Authorization Code + PKCE
          |
          v
      API Gateway (JWT + scopes)
          |
          v
      Backend Spring Boot (ECS)
          |
          +--> RDS PostgreSQL
          |
          +--> RabbitMQ (direct exchange)
                  |
                  +--> cola solicitud-creada --> Notificación
                  |
                  +--> cola solicitud-decidida --> Ticket
                  |
                  +--> DLX --> DLQ --> Auditor (log)

Rabbit Admin (REST independiente, :8082)
```

RabbitMQ corre en **EC2 + Docker** (3 nodos, colas quorum). Backend, notificaciones y admin en **ECS Fargate**. Diagrama: `arquitectura.mmd`.

## 3. Topología RabbitMQ

Nombres centralizados en `mensajeria` de cada `application.yml` (`MensajeriaProperties`). No van hardcodeados en el dominio.

| Recurso | Nombre |
| --- | --- |
| Exchange de negocio (direct) | `presupuestos.exchange` |
| Dead letter exchange (direct) | `presupuestos.dlx` |
| Cola notificaciones | `presupuestos.notificaciones.solicitud-creada` |
| Cola tickets | `presupuestos.tickets.solicitud-decidida` |
| DLQ | `presupuestos.dlq` |

| Exchange | Routing key | Cola destino |
| --- | --- | --- |
| `presupuestos.exchange` | `solicitud.creada` | `presupuestos.notificaciones.solicitud-creada` |
| `presupuestos.exchange` | `solicitud.decidida` | `presupuestos.tickets.solicitud-decidida` |
| `presupuestos.dlx` | `solicitud.dead` | `presupuestos.dlq` |

Declaración: `notificaciones-ms/.../mensajeria/RabbitTopologiaConfig.java` (beans `Queue` / `Exchange` / `Binding` por caso de uso). Colas **quorum** durables, con DLX, `x-delivery-limit` y TTL. El backend declara el exchange de publicación.

## 4. Flujo de eventos

- Al **crear** → publica `solicitud.creada` → listener de notificaciones.
- Al **decidir** (aprobar/rechazar) → publica `solicitud.decidida` → listener de tickets.

Productor: `backend/.../mensajeria/SolicitudEventoPublisher.java` (`@TransactionalEventListener(AFTER_COMMIT)`).  
Consumidores en `notificaciones-ms`: `SolicitudCreadaListener`, `SolicitudDecididaListener`, `DeadLetterListener`.

Si Rabbit falla al publicar, el negocio HTTP sigue; solo se registra el error.

## 5. Confiabilidad y entrega

- ACK **manual**, `prefetch: 1`.
- Error recuperable → NACK con requeue (hasta 3 intentos vía `x-delivery-count`).
- Error permanente / reintentos agotados / excepción inesperada → NACK sin requeue → DLQ.
- Política en `ManejoDeFallos`; el auditor loguea `x-death` en CloudWatch.

Demo forzada en el título de la solicitud:

| Marca | Efecto |
| --- | --- |
| `#temporal` | 3 reintentos y luego DLQ |
| `#permanente` | DLQ inmediata |

Listeners pausables: `GET/POST /api/listeners/...` en notificaciones-ms (útil para ver mensajes Ready en Management).

## 6. Clúster RabbitMQ

### 6.1 Local

`docker/rabbitmq/docker-compose.yml`: nodos `ep2-rabbitmq-1/2/3`, puertos AMQP `5672-5674`, Management `15672-15674`. Credenciales por defecto: `admin` / `admin123`.

### 6.2 AWS

Una EC2 con Docker Compose (3 contenedores). Management en `http://<IP_PUBLICA>:15672`. AMQP solo desde el SG de las tasks ECS.

Failover controlado (Session Manager):

```bash
sudo docker stop ep2-rabbitmq-2   # o el nodo que no exponga :15672
# cluster 2/3; colas quorum siguen; crear solicitud → ACK en CloudWatch
sudo docker start ep2-rabbitmq-2
```

Si se para `ep2-rabbitmq-1`, `:15672` cae; usar `:15673` / `:15674` o reiniciar el nodo 1.

**Nota:** los 3 nodos están en un solo host EC2: hay resiliencia a nivel de proceso/nodo Docker, no HA ante pérdida de toda la instancia.

## 7. Rabbit Admin REST API

Prefijo `/api/rabbitmq` (`rabbit-admin-ms`):

| Método | Ruta | Respuesta típica |
| --- | --- | --- |
| POST | `/api/rabbitmq/queues` | 201 + Location |
| GET | `/api/rabbitmq/queues/{nombre}` | 200 / 404 |
| DELETE | `/api/rabbitmq/queues/{nombre}` | 204 / 404 / 409 |
| DELETE | `/api/rabbitmq/queues/{nombre}/messages` | 200 |
| POST | `/api/rabbitmq/exchanges` | 201 |
| DELETE | `/api/rabbitmq/exchanges/{nombre}` | 204 / 404 |
| POST | `/api/rabbitmq/bindings` | 201 |
| DELETE | `/api/rabbitmq/bindings?exchange=&cola=&routingKey=` | 204 |

Controller delgado → `RabbitAdminService`. Recursos con prefijo `presupuestos.` protegidos (**409**). DTOs con `@Valid` → **400** si el nombre está vacío o la config es inválida.

## 8. Seguridad (EA1)

- Cognito Hosted UI, Authorization Code + PKCE.
- Grupos `trabajadores` / `administradores`; Lambda pre-token agrega scopes.
- API Gateway valida JWT/scopes; backend resource server.

| Ruta | Scope |
| --- | --- |
| `GET /presupuestos` | `presupuestos/read` |
| `POST` / `PUT` / `DELETE` | `presupuestos/write` |
| `POST /presupuestos/{id}/decision` | `presupuestos/decidir` |

## 9. Frontend

SPA **React** (Vite) en Amplify. Config runtime en `frontend/public/config.json` (no subir secretos). Local: `./deploy.sh --config-local` y `npm run dev`.

## 10. Ejecución

### AWS (entrega)

```bash
cd terraform && terraform apply
terraform output rabbitmq_management_url

./scripts/publicar-ecs.sh
./scripts/publicar-ms-ecs.sh notificaciones-ms
./scripts/publicar-ms-ecs.sh rabbit-admin-ms
./deploy.sh
```

| Pieza | Dónde |
| --- | --- |
| RabbitMQ 3 nodos | EC2 + Docker |
| backend / notificaciones / admin | ECS Fargate (`:8080` / `:8081` / `:8082`) |
| Frontend | Amplify |
| Auth / API / BD | Cognito, API Gateway, RDS |

### Local (desarrollo)

```bash
docker compose -f docker/rabbitmq/docker-compose.yml up -d --wait
# UI: http://localhost:15672

cd notificaciones-ms && ./mvnw spring-boot:run   # declara topología
cd rabbit-admin-ms && ./mvnw spring-boot:run
cd backend && ./mvnw spring-boot:run
./deploy.sh --config-local --api-local && cd frontend && npm run dev
```

Requisitos: Java 21+, Docker, Node.

### Pruebas

```bash
cd backend && ./mvnw test
cd notificaciones-ms && ./mvnw test
cd rabbit-admin-ms && ./mvnw test
```

## 11. Estructura del repositorio

| Ruta | Contenido |
| --- | --- |
| `backend/` | API presupuestos, JWT, productor de eventos |
| `frontend/` | SPA React |
| `notificaciones-ms/` | Consumidores (notificación, ticket, DLQ) |
| `rabbit-admin-ms/` | API REST de administración RabbitMQ |
| `docker/rabbitmq/` | Compose cluster 3 nodos |
| `terraform/` | Cognito, API GW, ECS, RDS, EC2 Rabbit, Amplify |
| `scripts/` | Publicación ECS / MS |

## 12. CI/CD

| Workflow | Propósito |
| --- | --- |
| `frontend_compile.yml` | Build / tests del frontend |
| `frontend_deploy.yml` | Despliegue Amplify |
| `backend_compile.yml` | `verify` del backend |
| `backend_deploy.yml` | Imagen y servicio ECS del backend |
| `microservicios_compile.yml` | Compilación de notificaciones / rabbit-admin |
| `user_token_ms_deploy.yml` | Lambda pre-token Cognito |

Tras un lab Academy nuevo, actualizar credenciales AWS y, si cambió la IP del backend, reapuntar la integración del API Gateway.
