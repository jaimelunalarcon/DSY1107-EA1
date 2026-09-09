/**
 * Authorization Code Flow + PKCE, alineado al diagrama de Auth0
 * (en este proyecto el "Auth0 Tenant" es Amazon Cognito Hosted UI).
 *
 * Mapa de pasos → dónde está en el código:
 *  1  beginLogin()                 App.tsx (clic del usuario)
 *  2  createPkce()                 pkce.ts
 *  3  beginLogin() → /authorize    este archivo
 *  4  Cognito redirige al login    este archivo (comentario PASO 4)
 *  5  Usuario escribe clave        Hosted UI (no hay código React)
 *  6  Cognito vuelve con ?code=    completeLoginFromCallback()
 *  7  POST /oauth2/token           exchangeCodeForTokens()
 *  8  Cognito valida PKCE          servidor Cognito (no hay código React)
 *  9  Recibimos id_token + access  exchangeCodeForTokens()
 * 10  fetch API con Bearer         api-test-panel.tsx
 * 11  API responde                 api-test-panel.tsx
 */

import { getCognitoConfig } from "../config.ts";
import { createPkce } from "./pkce.ts";
import {
  OAUTH_STATE_KEY,
  PKCE_VERIFIER_KEY,
  type AuthTokens,
  clearAuthStorage,
  emailFromIdToken,
} from "./tokens.ts";

/**
 * PASO 1 (disparado desde App) + PASO 2 + PASO 3
 *
 * PASO 4 — ¿dónde está?
 * No hay un `return <LoginForm />` en React. El paso 4 lo ejecuta Cognito:
 * después de recibir GET /oauth2/authorize, el authorization server responde
 * al NAVEGADOR con un 302 hacia la Hosted UI (pantalla email/password).
 * Ese redirect ocurre entre el PASO 3 (esta función) y el PASO 5 (el usuario).
 */
export async function beginLogin(): Promise<void> {
  const cognitoConfig = getCognitoConfig();
  // PASO 2 — generar code_verifier y code_challenge
  const { codeVerifier, codeChallenge } = await createPkce();
  const state = crypto.randomUUID();

  sessionStorage.setItem(PKCE_VERIFIER_KEY, codeVerifier);
  sessionStorage.setItem(OAUTH_STATE_KEY, state);

  const params = new URLSearchParams({
    client_id: cognitoConfig.clientId,
    response_type: cognitoConfig.responseType,
    scope: cognitoConfig.scope,
    redirect_uri: cognitoConfig.redirectUri,
    state,
    code_challenge: codeChallenge,
    code_challenge_method: "S256",
  });

  // PASO 3 — Authorization Code Request + Code Challenge a /oauth2/authorize
  const authorizeUrl = `${cognitoConfig.domain}/oauth2/authorize?${params.toString()}`;

  // PASO 4 — Cognito (no React) redirige al usuario al prompt de login Hosted UI.
  // El browser deja esta SPA y muestra https://dsy1107-grupo33.auth.us-east-1.amazoncognito.com/...
  window.location.assign(authorizeUrl);
}

/**
 * PASO 6 — Cognito redirige a redirect_uri con ?code=...&state=...
 * Esta función se llama al cargar la SPA si la URL trae el code.
 */
export async function completeLoginFromCallback(): Promise<AuthTokens | null> {
  const params = new URLSearchParams(window.location.search);
  const error = params.get("error");
  const errorDescription = params.get("error_description");

  if (error) {
    throw new Error(errorDescription || error);
  }

  const code = params.get("code");
  const state = params.get("state");
  if (!code) return null;

  const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);
  if (!state || state !== savedState) {
    throw new Error("El parámetro state no coincide. Posible CSRF.");
  }

  const tokens = await exchangeCodeForTokens(code);
  window.history.replaceState({}, document.title, getCognitoConfig().redirectUri);
  return tokens;
}

/**
 * PASO 7 — la App envía code + code_verifier a /oauth2/token
 * PASO 8 — Cognito valida verifier vs challenge (servidor; no hay código aquí)
 * PASO 9 — Cognito responde con ID Token y Access Token
 */
async function exchangeCodeForTokens(code: string): Promise<AuthTokens> {
  const cognitoConfig = getCognitoConfig();
  const codeVerifier = sessionStorage.getItem(PKCE_VERIFIER_KEY);
  if (!codeVerifier) {
    throw new Error("No hay code_verifier. Vuelve a iniciar sesión.");
  }

  const body = new URLSearchParams({
    grant_type: "authorization_code",
    client_id: cognitoConfig.clientId,
    code,
    redirect_uri: cognitoConfig.redirectUri,
    code_verifier: codeVerifier,
  });

  const res = await fetch(`${cognitoConfig.domain}/oauth2/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body,
  });

  const payload = (await res.json()) as {
    access_token?: string;
    id_token?: string;
    refresh_token?: string;
    error?: string;
    error_description?: string;
  };

  if (!res.ok || !payload.access_token || !payload.id_token) {
    throw new Error(
      payload.error_description ||
        payload.error ||
        "Cognito no entregó tokens.",
    );
  }

  sessionStorage.removeItem(PKCE_VERIFIER_KEY);
  sessionStorage.removeItem(OAUTH_STATE_KEY);

  return {
    accessToken: payload.access_token,
    idToken: payload.id_token,
    refreshToken: payload.refresh_token,
    email: emailFromIdToken(payload.id_token),
  };
}

export function beginLogout(): void {
  clearAuthStorage();
  const cognitoConfig = getCognitoConfig();
  const params = new URLSearchParams({
    client_id: cognitoConfig.clientId,
    logout_uri: cognitoConfig.redirectUri,
  });
  window.location.assign(`${cognitoConfig.domain}/logout?${params.toString()}`);
}
