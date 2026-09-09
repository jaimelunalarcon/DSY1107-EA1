data "aws_region" "current" {}

# Integración de /datos (método y URI fijos). Tras publicar-ecs.sh apunta a
# http://IP:8080/datos. ignore_changes evita que apply la devuelva a mindicador.
resource "aws_apigatewayv2_integration" "backend" {
  api_id                 = aws_apigatewayv2_api.api_manager.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "GET"
  integration_uri        = var.backend_url
  payload_format_version = "1.0"
  timeout_milliseconds   = 29000

  lifecycle {
    ignore_changes = [integration_uri]
  }
}

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

resource "aws_apigatewayv2_route" "datos" {
  api_id             = aws_apigatewayv2_api.api_manager.id
  route_key          = "GET /datos"
  target             = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito.id
}

resource "aws_apigatewayv2_route" "publico_datos" {
  api_id    = aws_apigatewayv2_api.api_manager.id
  route_key = "GET /publico/datos"
  target    = "integrations/${aws_apigatewayv2_integration.backend.id}"
}

# CRUD /productos: integraciones ANY separadas (no reutilizar la de /datos).
resource "aws_apigatewayv2_integration" "productos_coleccion" {
  api_id                 = aws_apigatewayv2_api.api_manager.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "${var.backend_url}/productos"
  payload_format_version = "1.0"
  timeout_milliseconds   = 29000

  lifecycle {
    ignore_changes = [integration_uri]
  }
}

resource "aws_apigatewayv2_integration" "productos_elemento" {
  api_id                 = aws_apigatewayv2_api.api_manager.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "${var.backend_url}/productos/{proxy}"
  payload_format_version = "1.0"
  timeout_milliseconds   = 29000

  lifecycle {
    ignore_changes = [integration_uri]
  }
}

locals {
  rutas_productos = {
    "GET /productos"             = { integracion = aws_apigatewayv2_integration.productos_coleccion.id }
    "POST /productos"            = { integracion = aws_apigatewayv2_integration.productos_coleccion.id }
    "GET /productos/{proxy+}"    = { integracion = aws_apigatewayv2_integration.productos_elemento.id }
    "PUT /productos/{proxy+}"    = { integracion = aws_apigatewayv2_integration.productos_elemento.id }
    "DELETE /productos/{proxy+}" = { integracion = aws_apigatewayv2_integration.productos_elemento.id }
  }
}

resource "aws_apigatewayv2_route" "productos" {
  for_each = local.rutas_productos

  api_id             = aws_apigatewayv2_api.api_manager.id
  route_key          = each.key
  target             = "integrations/${each.value.integracion}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito.id
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
