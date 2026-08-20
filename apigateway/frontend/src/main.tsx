import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { AuthProvider } from "react-oidc-context";
import App from "./App.tsx";
import { cognitoConfig } from "./config.ts";
import "./index.css";

const cognitoAuthConfig = {
  authority: cognitoConfig.authority,
  client_id: cognitoConfig.clientId,
  redirect_uri: cognitoConfig.redirectUri,
  response_type: cognitoConfig.responseType,
  scope: cognitoConfig.scope,
};

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <AuthProvider {...cognitoAuthConfig}>
      <App />
    </AuthProvider>
  </StrictMode>
);