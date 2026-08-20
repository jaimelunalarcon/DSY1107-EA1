import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { AuthProvider } from "react-oidc-context";
import App from "./App.tsx";
import "./index.css";

const cognitoAuthConfig = {
  authority:
    "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_2UkerqkVs",
  client_id: "7a4m65ujlo1q91cdamhvilnv0a",
  redirect_uri: "http://localhost:5173/",
  response_type: "code",
  scope: "openid email profile",
};

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <AuthProvider {...cognitoAuthConfig}>
      <App />
    </AuthProvider>
  </StrictMode>
);