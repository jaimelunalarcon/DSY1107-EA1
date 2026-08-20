/**
 * PASO 2 del diagrama (Authorization Code + PKCE)
 *
 * La App genera:
 * - code_verifier: secreto aleatorio (nunca viaja en el /authorize)
 * - code_challenge: SHA-256 del verifier, en Base64URL (sí viaja en /authorize)
 *
 * Cognito guardará el challenge. Más tarde, en el PASO 7, exigirá el verifier.
 */

function toBase64Url(bytes: Uint8Array): string {
  let binary = "";
  bytes.forEach((b) => {
    binary += String.fromCharCode(b);
  });
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function randomVerifier(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(64));
  return toBase64Url(bytes);
}

async function sha256Base64Url(value: string): Promise<string> {
  const data = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return toBase64Url(new Uint8Array(digest));
}

export async function createPkce(): Promise<{
  codeVerifier: string;
  codeChallenge: string;
}> {
  const codeVerifier = randomVerifier();
  const codeChallenge = await sha256Base64Url(codeVerifier);
  return { codeVerifier, codeChallenge };
}
