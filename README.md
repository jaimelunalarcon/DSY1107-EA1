# DSY1107-EA1 — Cognito + API Gateway + SPA + presupuestos

Evaluación académica (DSY1107) sobre autenticación OAuth 2.0 / OIDC con **Amazon Cognito**, exposición de un API vía **API Gateway HTTP**, backend **Spring Boot en ECS**, y consumo desde una **SPA React** con flujo **Authorization Code + PKCE**.

Dominio de negocio: **solicitudes de presupuesto** (trabajador crea/edita; administrador aprueba/rechaza) con scopes Cognito.

---

## Arquitectura

```
Usuario
  │
  ▼
SPA React (Vite)  ──login OIDC/PKCE──►  Cognito Hosted UI
  │                                         │
  │◄──────── code + tokens ─────────────────┘
  │
  ├── Cognito userInfo / GetUser
  └── /presupuestos + Bearer + scopes
        → API Gateway (JWT) → ECS Spring Boot → RDS
```

Infra con **Terraform** (`us-east-1`).

---

## Stack

| Capa | Tecnología |
|------|------------|
| Infra | Terraform + AWS |
| Identidad | Cognito User Pool + Hosted UI + Lambda Pre Token Generation |
| API | API Gateway HTTP + JWT authorizer + scopes |
| Backend | Spring Boot 4 / Java 21 en ECS Fargate |
| Datos | RDS PostgreSQL + Flyway |
| Frontend | React 19, TypeScript, Vite, Tailwind |

---

## API Gateway

| Ruta | Scope |
|------|--------|
| `GET /presupuestos` | `presupuestos/read` |
| `POST /presupuestos` | `presupuestos/write` |
| `PUT/DELETE /presupuestos/{id}` | `presupuestos/write` |
| `POST /presupuestos/{id}/decision` | `presupuestos/decidir` |

Sin token → **401**. Sin scope → **403**.

---

## Cómo levantarlo

```bash
cd terraform
terraform init
terraform apply -var='crear_ruta_internet=false'   # si el lab ya tiene la ruta IGW

./scripts/publicar-ecs.sh    # imagen + reapunta integraciones al ECS
./deploy.sh --config-local   # config.json para localhost
# o ./deploy.sh para Amplify

cd frontend && npm install && npm run dev
```

Crear usuarios en Cognito y asignarlos a grupos `trabajadores` o `administradores`.

---

## Comandos útiles

```bash
./scripts/publicar-ecs.sh
./deploy.sh
./sincronizar-github.sh
terraform -chdir=terraform output -raw url_presupuestos
aws logs tail /ecs/dsy1107-backend-grupo33 --follow
```

---

## Curso

**DSY1107** — EA1 · Cognito PKCE · Amplify · ECS · presupuestos + scopes
