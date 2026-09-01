export type AuthTokens = {
  accessToken: string;
  idToken: string;
  refreshToken?: string;
  email: string;
};

const TOKENS_KEY = "cognito_tokens";
export const PKCE_VERIFIER_KEY = "pkce_code_verifier";
export const OAUTH_STATE_KEY = "oauth_state";

export function saveTokens(tokens: AuthTokens): void {
  sessionStorage.setItem(TOKENS_KEY, JSON.stringify(tokens));
}

export function loadTokens(): AuthTokens | null {
  const raw = sessionStorage.getItem(TOKENS_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthTokens;
  } catch {
    return null;
  }
}

export function clearAuthStorage(): void {
  sessionStorage.removeItem(TOKENS_KEY);
  sessionStorage.removeItem(PKCE_VERIFIER_KEY);
  sessionStorage.removeItem(OAUTH_STATE_KEY);
}

/** Lee el claim email del ID Token (JWT) sin librerías. */
export function emailFromIdToken(idToken: string): string {
  const payload = idToken.split(".")[1];
  if (!payload) return "cuenta autenticada";
  const json = JSON.parse(
    atob(payload.replace(/-/g, "+").replace(/_/g, "/")),
  ) as { email?: string };
  return json.email ?? "cuenta autenticada";
}
