data "aws_region" "current" {}

# JWT authorizer (Cognito). Las rutas de negocio apuntan a ECS via
# publicar-ecs.sh (ignore_changes en integration_uri).
resource "aws_apigatewayv2_authorizer" "cognito" {
  api_id           = aws_apigatewayv2_api.api_manager.id
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  name             = "cognito-jwt"

  jwt_configuration {
    audience = [aws_cognito_user_pool_client.spa.id]
    issuer   = "https://cognito-idp.${data.aws_region.current.name}.amazonaws.com/${aws_cognito_user_pool.pool.id}"
  }
}

# Solicitudes de presupuesto: integraciones ANY hacia el backend Spring.
resource "aws_apigatewayv2_integration" "presupuestos_coleccion" {
  api_id                 = aws_apigatewayv2_api.api_manager.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "${var.backend_url}/presupuestos"
  payload_format_version = "1.0"
  timeout_milliseconds   = 29000

  lifecycle {
    ignore_changes = [integration_uri]
  }
}

resource "aws_apigatewayv2_integration" "presupuestos_elemento" {
  api_id                 = aws_apigatewayv2_api.api_manager.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "${var.backend_url}/presupuestos/{proxy}"
  payload_format_version = "1.0"
  timeout_milliseconds   = 29000

  lifecycle {
    ignore_changes = [integration_uri]
  }
}

locals {
  rutas_presupuestos = {
    "GET /presupuestos"             = { scope = "presupuestos/read",    integracion = aws_apigatewayv2_integration.presupuestos_coleccion.id }
    "POST /presupuestos"            = { scope = "presupuestos/write",   integracion = aws_apigatewayv2_integration.presupuestos_coleccion.id }
    "GET /presupuestos/{proxy+}"    = { scope = "presupuestos/read",    integracion = aws_apigatewayv2_integration.presupuestos_elemento.id }
    "PUT /presupuestos/{proxy+}"    = { scope = "presupuestos/write",   integracion = aws_apigatewayv2_integration.presupuestos_elemento.id }
    "DELETE /presupuestos/{proxy+}" = { scope = "presupuestos/write",   integracion = aws_apigatewayv2_integration.presupuestos_elemento.id }
    "POST /presupuestos/{proxy+}"   = { scope = "presupuestos/decidir", integracion = aws_apigatewayv2_integration.presupuestos_elemento.id }
  }
}

resource "aws_apigatewayv2_route" "presupuestos" {
  for_each = local.rutas_presupuestos

  api_id               = aws_apigatewayv2_api.api_manager.id
  route_key            = each.key
  target               = "integrations/${each.value.integracion}"
  authorization_type   = "JWT"
  authorizer_id        = aws_apigatewayv2_authorizer.cognito.id
  authorization_scopes = [each.value.scope]
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.api_manager.id
  name        = "$default"
  auto_deploy = true
}

resource "aws_apigatewayv2_stage" "dev" {
  api_id      = aws_apigatewayv2_api.api_manager.id
  name        = "dev"
  auto_deploy = true
}
