export const cognitoConfig = {
  authority:
    "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_EGliVIr5e",
  clientId: "676iaqis437v4inkqjtvoot06p",
  // Local (Vite) o Amplify: debe coincidir con un callback_url de Cognito.
  redirectUri: `${window.location.origin}/`,
  responseType: "code" as const,
  scope: "openid email profile",
  domain: "https://dsy1107-grupo33.auth.us-east-1.amazoncognito.com",
  region: "us-east-1",
};

export const apiConfig = {
  baseUrl: "https://kaqk2cvuw6.execute-api.us-east-1.amazonaws.com",
};
