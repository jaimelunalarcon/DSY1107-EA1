/**
 * Pre Token Generation V2: traduce grupos Cognito a scopes del access token.
 *
 * trabajadores  -> presupuestos/read + presupuestos/write
 * administradores -> presupuestos/read + presupuestos/decidir
 */

const SCOPES_POR_GRUPO = {
  trabajadores: ["presupuestos/read", "presupuestos/write"],
  administradores: ["presupuestos/read", "presupuestos/decidir"],
};

export const handler = async (event) => {
  const grupos = event.request.groupConfiguration?.groupsToOverride ?? [];
  const scopes = [
    ...new Set(grupos.flatMap((grupo) => SCOPES_POR_GRUPO[grupo] ?? [])),
  ];

  console.log(
    JSON.stringify({
      usuario: event.request.userAttributes?.email ?? event.userName,
      grupos,
      scopes,
    }),
  );

  event.response = {
    claimsAndScopeOverrideDetails: {
      accessTokenGeneration: {
        scopesToAdd: scopes,
      },
    },
  };

  return event;
};
