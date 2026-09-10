export type AppConfig = {
  region: string;
  cognitoDomain: string;
  clientId: string;
  redirectUri: string;
  apiUrl: string;
};

let cached: AppConfig | null = null;

function normalizeDomain(domain: string): string {
  return domain.startsWith("http://") || domain.startsWith("https://")
    ? domain
    : `https://${domain}`;
}

/** Carga /config.json (Vite copia public/ al build). */
export async function loadConfig(): Promise<AppConfig> {
  const res = await fetch("/config.json", { cache: "no-store" });
  if (!res.ok) {
    throw new Error(
      "Falta configurar el front: no existe /config.json. Ejecuta ./deploy.sh --config-local",
    );
  }
  const data = (await res.json()) as AppConfig;
  cached = {
    region: data.region,
    cognitoDomain: normalizeDomain(data.cognitoDomain),
    clientId: data.clientId,
    redirectUri: data.redirectUri,
    apiUrl: data.apiUrl,
  };
  return cached;
}

export function getConfig(): AppConfig {
  if (!cached) {
    throw new Error("Config no cargada: llama loadConfig() antes de arrancar la app.");
  }
  return cached;
}

/** Forma usada por auth y el panel de API. */
export function getCognitoConfig() {
  const c = getConfig();
  return {
    clientId: c.clientId,
    redirectUri: c.redirectUri,
    responseType: "code" as const,
    scope: "openid email profile aws.cognito.signin.user.admin",
    domain: c.cognitoDomain,
    region: c.region,
  };
}

export function getApiConfig() {
  return { baseUrl: getConfig().apiUrl };
}
