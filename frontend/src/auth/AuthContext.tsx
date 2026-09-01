import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import {
  beginLogin,
  beginLogout,
  completeLoginFromCallback,
} from "./cognito-auth.ts";
import { loadTokens, saveTokens, type AuthTokens } from "./tokens.ts";

type AuthStatus = "loading" | "anonymous" | "authenticated";

type AuthContextValue = {
  status: AuthStatus;
  tokens: AuthTokens | null;
  error: string | null;
  login: () => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [tokens, setTokens] = useState<AuthTokens | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      try {
        // PASO 6: si Cognito nos devolvió ?code=, intercambiamos tokens (pasos 7–9)
        const fromCallback = await completeLoginFromCallback();
        if (cancelled) return;

        if (fromCallback) {
          saveTokens(fromCallback);
          setTokens(fromCallback);
          setStatus("authenticated");
          return;
        }

        const stored = loadTokens();
        if (stored) {
          setTokens(stored);
          setStatus("authenticated");
          return;
        }

        setStatus("anonymous");
      } catch (err) {
        if (cancelled) return;
        setError(
          err instanceof Error ? err.message : "Error al completar el login.",
        );
        setStatus("anonymous");
      }
    }

    void bootstrap();
    return () => {
      cancelled = true;
    };
  }, []);

  async function login() {
    setError(null);
    // PASO 1 → dispara PASO 2 y PASO 3 (luego Cognito hace el PASO 4)
    await beginLogin();
  }

  function logout() {
    beginLogout();
  }

  return (
    <AuthContext.Provider value={{ status, tokens, error, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error("useAuth debe usarse dentro de AuthProvider");
  }
  return value;
}
