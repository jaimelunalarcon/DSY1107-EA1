# DSY1107-EA1 — Cognito + API Gateway + SPA React

Evaluación académica (DSY1107, Grupo 33) sobre autenticación OAuth 2.0 / OIDC con **Amazon Cognito**, exposición de un API vía **API Gateway HTTP**, y consumo desde una **SPA React** con flujo **Authorization Code + PKCE** implementado a mano (sin librerías OIDC).

---

## ¿Qué es este proyecto?

El objetivo es demostrar, de punta a punta:

1. **Identidad** — User Pool de Cognito + Hosted UI (login por email).
2. **Autorización** — tokens JWT (access / id) vía Authorization Code + PKCE.
3. **API** — API Gateway como proxy hacia [mindicador.cl](https://mindicador.cl/api).
4. **Cliente** — SPA que inicia sesión, guarda tokens y prueba endpoints protegidos y públicos.

No hay backend propio ni Lambda: el “servicio” es un **HTTP proxy** hacia Mindicador. El valor del ejercicio está en Cognito, el authorizer JWT y el flujo OAuth en el navegador.

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
  ├── GET /oauth2/userInfo     → Cognito
  ├── Cognito GetUser          → Cognito IdP API
  ├── GET /datos + Bearer      → API Gateway (JWT) → mindicador.cl
  ├── GET /datos (sin token)   → API Gateway → 401
  └── GET /publico/datos       → API Gateway (público) → mindicador.cl
```

Infraestructura provisionada con **Terraform** (región `us-east-1`).

---

## Stack

| Capa | Tecnología |
|------|------------|
| Infra | Terraform + AWS provider |
| Identidad | Amazon Cognito User Pool + Hosted UI |
| API | API Gateway HTTP (v2) + JWT authorizer |
| Backend externo | mindicador.cl |
| Frontend | React 19, TypeScript, Vite 8, Tailwind CSS 4 |
| Auth en cliente | PKCE manual (`Web Crypto` + `fetch`) |

---

## Estructura del repositorio

```
DSY1107-EA1/
├── .github/workflows/    # CI: compile + deploy Amplify
├── frontend/             # SPA React (Vite)
│   ├── src/
│   │   ├── auth/         # PKCE + Cognito + AuthContext
│   │   ├── components/
│   │   ├── App.tsx
│   │   ├── config.ts     # loadConfig() desde /config.json
│   │   └── main.tsx
│   ├── public/
│   │   ├── config.example.json
│   │   └── config.json   # generado por ./deploy.sh (gitignored)
│   └── package.json
├── scripts/              # config-frontend.sh, publicar-amplify.sh
├── terraform/            # Infra AWS (Cognito, API GW, Amplify)
├── deploy.sh             # 1.2.9e: config + build + Amplify
└── sincronizar-github.sh # Volcar outputs de Terraform a vars de GitHub
```

---

## Flujo de autenticación (Authorization Code + PKCE)

El login sigue el diagrama estándar de Authorization Code + PKCE (equivalente al de Auth0, usando Cognito como authorization server).

| Paso | Qué ocurre | Dónde está en el código |
|------|------------|-------------------------|
| 1 | Usuario hace clic en login | `App.tsx` → `login()` |
| 2 | Se generan `code_verifier` y `code_challenge` | `src/auth/pkce.ts` |
| 3 | Redirect a `/oauth2/authorize` + challenge | `src/auth/cognito-auth.ts` → `beginLogin()` |
| 4 | Cognito muestra Hosted UI | **Servidor Cognito** (no React) |
| 5 | Usuario autenticación / consent | Hosted UI |
| 6 | Vuelve a `localhost:5173/?code=...` | `completeLoginFromCallback()` |
| 7 | `code` + `code_verifier` → `/oauth2/token` | `exchangeCodeForTokens()` |
| 8 | Cognito valida PKCE | **Servidor Cognito** |
| 9 | App recibe `access_token` e `id_token` | `exchangeCodeForTokens()` |
| 10–11 | Llamadas API con Bearer | `api-test-panel.tsx` |

**PKCE** permite que una SPA (cliente público, sin `client_secret`) use Authorization Code de forma segura: aunque alguien robe el `code` de la URL, no puede canjearlo sin el `code_verifier`.

---

## API Gateway

| Ruta | Auth | Comportamiento esperado |
|------|------|-------------------------|
| `GET /datos` | JWT (Cognito) | **200** con token válido; **401** sin token |
| `GET /publico/datos` | Ninguna | **200** (proxy a mindicador) |

CORS habilitado para `http://localhost:5173` y la URL de Amplify (tras `terraform apply`).

La SPA incluye un panel de pruebas con botones:

- `/oauth2/userInfo`
- `Cognito GetUser`
- `/datos con token`
- `/datos sin token`
- `/publico/datos`

Cada uno muestra status HTTP y el JSON de respuesta.

---

## Cómo levantarlo

### 1. Infraestructura (AWS)

Requiere credenciales AWS válidas (en AWS Academy: las del Learner Lab).

```bash
cd terraform
terraform init
terraform apply
terraform output
```

### 2. Actualizar el frontend

Genera `frontend/public/config.json` desde Terraform (no edites IDs a mano):

```bash
./deploy.sh --config-local
```

Eso deja `redirectUri` en `http://localhost:5173/` (barra final incluida).

### 3. Usuario en Cognito

Como el pool solo permite creación por admin:

1. Consola AWS → Cognito → User Pool → Create user.
2. Email verificado.
3. Contraseña **permanente** (no temporal / `FORCE_CHANGE_PASSWORD`).

### 4. SPA

```bash
cd frontend
npm install
npm run dev
```

Abrir `http://localhost:5173/` e iniciar sesión.

---

## 1.2.9e — Publicar en Amplify

Tras `terraform apply` (Cognito + API + Amplify ya creados):

```bash
# Credenciales AWS activas (Academy / Free Tier)
./deploy.sh
```

Eso:

1. Escribe `frontend/public/config.json` con `redirectUri` = URL de Amplify  
2. Compila el front (`npm run build`)  
3. Sube el zip a Amplify (`create-deployment` → PUT → `start-deployment`)

Abre la URL:

```bash
terraform -chdir=terraform output -raw amplify_url
```

Para volver a desarrollar en local:

```bash
./deploy.sh --config-local
cd frontend && npm run dev
```

### Errores típicos (guía)

| Síntoma | Causa |
|---------|--------|
| `redirect_mismatch` | Bundle con config de localhost en Amplify (o al revés) |
| 404 en rutas SPA | Falta `custom_rule` en Amplify |
| CORS bloqueado | Origen Amplify sin barra en Cognito; **sin** barra en CORS |
| Zip 404 en `/` | Se comprimió la carpeta `dist`, no su **contenido** |

### CI

```bash
./sincronizar-github.sh   # vars de GitHub desde Terraform
# Secrets: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN
```

Push a `frontend/**` dispara `frontend_deploy.yml`.

---

## 1.3.9 — Backend en ECS Fargate

Terraform crea ECR, cluster, task, service y RDS. La **imagen** la publica el script (no va en el tfstate).

```bash
# Lab activo + Docker + Java 21
cd terraform
terraform apply          # RDS tarda 5–10 min; el servicio puede reintentar sin imagen

# Desde la raíz del repo:
./scripts/publicar-ecs.sh
```

Eso: `mvnw verify` → `docker build --platform linux/amd64` → ECR → redespliegue → reapunta las 3 integraciones del API Gateway a `http://IP:8080/...`.

Comprobar:

```bash
terraform -chdir=terraform output -raw probar_sin_token   # HTTP 401
aws logs tail /ecs/dsy1107-backend-grupo33 --follow
```

Con token (login en el front): `GET /datos` → 200. Health directo: `http://IP:8080/actuator/health`.

---

## Nota sobre AWS Academy

En entornos académicos los recursos suelen borrarse al cerrar el lab. El flujo típico en cada sesión es:

1. Configurar credenciales nuevas.
2. `terraform apply`
3. `./deploy.sh --config-local` (o `./deploy.sh` para Amplify)
4. Recrear el usuario en Cognito si hace falta.
5. Probar la SPA.

---

## Qué ya está hecho

- [x] Terraform: Cognito + Hosted UI + cliente SPA
- [x] Terraform: API Gateway HTTP, CORS, JWT authorizer
- [x] Rutas `/datos` (protegida) y `/publico/datos` (pública)
- [x] SPA React con UI de login y sesión
- [x] Authorization Code + PKCE **manual** (sin `react-oidc-context`)
- [x] Panel para consumir APIs con / sin token
- [x] Terraform: Amplify (1.2.9e)
- [x] `./deploy.sh` local + GitHub Actions deploy
- [x] Backend Spring Boot (`backend/`)
- [x] Terraform: RDS + ECS Fargate (1.3.9)
- [x] `scripts/publicar-ecs.sh` + workflow `backend_deploy.yml`

---

## Comandos útiles

```bash
# Infra
cd terraform && terraform plan
cd terraform && terraform apply
cd terraform && terraform output

# Frontend local
./deploy.sh --config-local
cd frontend && npm run dev

# Amplify (1.2.9e)
./deploy.sh

# Backend ECS (1.3.9)
cd terraform && terraform apply
./scripts/publicar-ecs.sh
```

---

## Curso

**DSY1107** — Evaluación EA1 · Grupo 33 · 1.2.9 / 1.2.9e Amplify · 1.3.9 ECS
