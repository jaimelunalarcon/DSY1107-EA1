import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { handler } from "./index.mjs";

describe("user-token-ms", () => {
  it("trabajador recibe read+write", async () => {
    const out = await handler({
      version: "2",
      request: {
        userAttributes: { email: "t@duoc.cl" },
        groupConfiguration: { groupsToOverride: ["trabajadores"] },
      },
      response: {},
    });
    assert.deepEqual(
      out.response.claimsAndScopeOverrideDetails.accessTokenGeneration.scopesToAdd,
      ["presupuestos/read", "presupuestos/write"],
    );
  });

  it("administrador recibe read+decidir", async () => {
    const out = await handler({
      version: "2",
      request: {
        userAttributes: { email: "a@duoc.cl" },
        groupConfiguration: { groupsToOverride: ["administradores"] },
      },
      response: {},
    });
    assert.deepEqual(
      out.response.claimsAndScopeOverrideDetails.accessTokenGeneration.scopesToAdd,
      ["presupuestos/read", "presupuestos/decidir"],
    );
  });
});
