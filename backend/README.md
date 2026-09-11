# Backend — solicitudes de presupuesto

Spring Boot detrás del API Gateway. Expone `/presupuestos` (CRUD + decisión admin).
La autenticación JWT y los scopes (`presupuestos/read|write|decidir`) los exige el Gateway; este servicio no valida el token.

## API

| Método | Ruta | Qué hace |
|--------|------|----------|
| `GET` | `/presupuestos` | Listar |
| `GET` | `/presupuestos/{id}` | Obtener una |
| `POST` | `/presupuestos` | Crear (trabajador) |
| `PUT` | `/presupuestos/{id}` | Editar si está `PENDIENTE` |
| `DELETE` | `/presupuestos/{id}` | Eliminar si está `PENDIENTE` |
| `POST` | `/presupuestos/{id}/decision` | Aprobar / rechazar (admin) |

Health: `GET /actuator/health` → `{"status":"UP"}`.

## Local

```bash
./mvnw spring-boot:run
# o con RDS:
set -a; eval "$(terraform -chdir=../terraform output -raw backend_env)"; set +a
./mvnw spring-boot:run
```

Pruebas (H2 en memoria):

```bash
./mvnw test
```

## Despliegue

```bash
./scripts/publicar-ecs.sh
```
