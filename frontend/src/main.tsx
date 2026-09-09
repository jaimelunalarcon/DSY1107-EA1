import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App.tsx";
import { AuthProvider } from "./auth/AuthContext.tsx";
import { loadConfig } from "./config.ts";
import "./index.css";

async function bootstrap() {
  const root = document.getElementById("root")!;
  try {
    await loadConfig();
    createRoot(root).render(
      <StrictMode>
        <AuthProvider>
          <App />
        </AuthProvider>
      </StrictMode>,
    );
  } catch (err) {
    const message =
      err instanceof Error ? err.message : "No se pudo cargar config.json";
    root.innerHTML = `<pre style="font:14px/1.4 system-ui;padding:2rem;white-space:pre-wrap">${message}</pre>`;
  }
}

void bootstrap();
