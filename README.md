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
│   │   ├── config.ts     # IDs/URLs (local o generado en CI)
│   │   └── main.tsx
│   └── package.json
├── scripts/              # config-frontend.sh, publicar-amplify.sh
├── terraform/            # Infra AWS (Cognito, API GW, Amplify)
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

Copia los outputs a `frontend/src/config.ts`:

- `cognito_user_pool_id` → `authority`
- `cognito_client_id` → `clientId`
- `cognito_domain` → `domain` (con `https://`)
- `api_base_url` → `apiConfig.baseUrl`

`redirectUri` usa `window.location.origin` (local o Amplify). Debe existir en los `callback_urls` de Cognito.

O genera el archivo con:

```bash
export REGION="$(terraform -chdir=terraform output -raw aws_region)"
export USER_POOL_ID="$(terraform -chdir=terraform output -raw cognito_user_pool_id)"
export CLIENT_ID="$(terraform -chdir=terraform output -raw cognito_client_id)"
export COGNITO_DOMAIN="$(terraform -chdir=terraform output -raw cognito_domain)"
export API_URL="$(terraform -chdir=terraform output -raw api_base_url)"
./scripts/config-frontend.sh
```

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

## Nota sobre AWS Academy

En entornos académicos los recursos suelen borrarse al cerrar el lab. El flujo típico en cada sesión es:

1. Configurar credenciales nuevas.
2. `terraform apply`
3. Actualizar `config.ts` con los nuevos IDs/URLs.
4. Recrear el usuario en Cognito.
5. Probar la SPA.

El código de la app no cambia; solo los identificadores de AWS.

---

## Qué ya está hecho

- [x] Terraform: Cognito + Hosted UI + cliente SPA
- [x] Terraform: API Gateway HTTP, CORS, JWT authorizer
- [x] Rutas `/datos` (protegida) y `/publico/datos` (pública)
- [x] SPA React con UI de login y sesión
- [x] Authorization Code + PKCE **manual** (sin `react-oidc-context`)
- [x] Panel para consumir APIs con / sin token
- [x] Terraform: Amplify (deploy manual / CI)
- [x] GitHub Actions: compile + deploy a Amplify

---

## Deploy en Amplify (CI)

Tras `terraform apply`, sincroniza variables de GitHub:

```bash
./sincronizar-github.sh
```

Configura los secrets `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN` (Academy) en el repositorio.

Deploy local del build:

```bash
cd frontend && npm run build
APP_ID="$(terraform -chdir=terraform output -raw amplify_app_id)"
./scripts/publicar-amplify.sh "$APP_ID" main frontend/dist
```

---

## Comandos útiles

```bash
# Infra
cd terraform && terraform plan
cd terraform && terraform apply
cd terraform && terraform output

# Frontend
cd frontend && npm run dev
cd frontend && npm run build
```

---

## Curso

**DSY1107** — Evaluación EA1 · Grupo 33
