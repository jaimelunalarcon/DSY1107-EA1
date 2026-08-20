import { useState } from "react";
import { clsx } from "clsx";
import type { User } from "oidc-client-ts";
import { apiConfig, cognitoConfig } from "../config.ts";
import { Button } from "./button";
import { Container } from "./container";

type ApiResult = {
  label: string;
  status: number | null;
  body: string;
  ok: boolean;
};

type ApiTestPanelProps = {
  user: User | null | undefined;
};

async function readResponse(res: Response) {
  const text = await res.text();
  try {
    return JSON.stringify(JSON.parse(text), null, 2);
  } catch {
    return text || "(sin cuerpo)";
  }
}

export function ApiTestPanel({ user }: ApiTestPanelProps) {
  const [loading, setLoading] = useState<string | null>(null);
  const [result, setResult] = useState<ApiResult | null>(null);

  const accessToken = user?.access_token;

  async function runTest(
    label: string,
    request: () => Promise<Response>,
  ) {
    setLoading(label);
    try {
      const res = await request();
      const body = await readResponse(res);
      setResult({
        label,
        status: res.status,
        body,
        ok: res.ok,
      });
    } catch (error) {
      setResult({
        label,
        status: null,
        body:
          error instanceof Error
            ? error.message
            : "Error de red o CORS al llamar el endpoint.",
        ok: false,
      });
    } finally {
      setLoading(null);
    }
  }

  const tests = [
    {
      id: "userInfo",
      label: "/oauth2/userInfo",
      variant: "primary" as const,
      disabled: !accessToken,
      action: () =>
        runTest("/oauth2/userInfo", () =>
          fetch(`${cognitoConfig.domain}/oauth2/userInfo`, {
            headers: { Authorization: `Bearer ${accessToken}` },
          }),
        ),
    },
    {
      id: "getUser",
      label: "Cognito GetUser",
      variant: "primary" as const,
      disabled: !accessToken,
      action: () =>
        runTest("Cognito GetUser", () =>
          fetch(`https://cognito-idp.${cognitoConfig.region}.amazonaws.com/`, {
            method: "POST",
            headers: {
              "Content-Type": "application/x-amz-json-1.1",
              "X-Amz-Target": "AWSCognitoIdentityProviderService.GetUser",
            },
            body: JSON.stringify({ AccessToken: accessToken }),
          }),
        ),
    },
    {
      id: "datosConToken",
      label: "/datos con token",
      variant: "primary" as const,
      disabled: !accessToken,
      action: () =>
        runTest("/datos con token", () =>
          fetch(`${apiConfig.baseUrl}/datos`, {
            headers: { Authorization: `Bearer ${accessToken}` },
          }),
        ),
    },
    {
      id: "datosSinToken",
      label: "/datos sin token",
      variant: "danger" as const,
      disabled: false,
      action: () =>
        runTest("/datos sin token", () =>
          fetch(`${apiConfig.baseUrl}/datos`),
        ),
    },
    {
      id: "publicoDatos",
      label: "/publico/datos",
      variant: "primary" as const,
      disabled: false,
      action: () =>
        runTest("/publico/datos", () =>
          fetch(`${apiConfig.baseUrl}/publico/datos`),
        ),
    },
  ];

  return (
    <section className="border-t border-black/5 bg-white py-16 sm:py-24">
      <Container>
        <h2 className="text-sm font-semibold tracking-widest text-gray-500 uppercase">
          APIs 
        </h2>

        <div className="mt-8 flex flex-wrap gap-3">
          {tests.map((test) => (
            <Button
              key={test.id}
              variant={test.variant}
              disabled={test.disabled || loading !== null}
              onClick={test.action}
            >
              {loading === test.label ? "Cargando…" : test.label}
            </Button>
          ))}
        </div>

        {result ? (
          <div
            className={clsx(
              "mt-8 overflow-hidden rounded-xl ring-1 ring-black/5",
              result.ok
                ? "border-l-4 border-l-emerald-500"
                : "border-l-4 border-l-red-500",
            )}
          >
            <div className="border-b border-black/5 bg-gray-50 px-4 py-3 font-mono text-sm text-gray-950">
              {result.label} →{" "}
              {result.status !== null
                ? `HTTP ${result.status}`
                : "Error de red"}
            </div>
            <pre className="max-h-[28rem] overflow-auto bg-gray-100 p-4 font-mono text-xs/6 text-gray-800 sm:text-sm/6">
              {result.body}
            </pre>
          </div>
        ) : (
          <p className="mt-8 text-sm/6 text-gray-500">
            Elige un endpoint para ver el status HTTP y el JSON de respuesta.
          </p>
        )}
      </Container>
    </section>
  );
}
