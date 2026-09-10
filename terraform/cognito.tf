resource "aws_cognito_user_pool" "pool" {
  name = "dsy1107-${var.estudiante}"

  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]
  password_policy {
    minimum_length    = 8
    require_lowercase = true
    require_uppercase = true
    require_numbers   = true
    require_symbols   = false
  }
  admin_create_user_config {
    allow_admin_create_user_only = true
  }

  # V2 del trigger Pre Token Generation (scopes en el access token).
  user_pool_tier = "ESSENTIALS"

  lambda_config {
    pre_token_generation_config {
      lambda_arn     = aws_lambda_function.user_token_ms.arn
      lambda_version = "V2_0"
    }
  }
}

resource "aws_cognito_user_pool_domain" "hosted_ui" {
  domain       = "dsy1107-${var.estudiante}"
  user_pool_id = aws_cognito_user_pool.pool.id
  managed_login_version = 1
}

resource "aws_cognito_user_pool_client" "spa" {
  name                                 = "spa-react"
  user_pool_id                         = aws_cognito_user_pool.pool.id
  generate_secret                      = false
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  supported_identity_providers         = ["COGNITO"]

  # Los scopes presupuestos/* NO van aqui: los pone solo el Lambda segun grupo.
  allowed_oauth_scopes = [
    "openid",
    "email",
    "profile",
    "aws.cognito.signin.user.admin",
  ]

  callback_urls = [
    "http://localhost:5173/",
    "${local.url_amplify}/",
  ]
  logout_urls = [
    "http://localhost:5173/",
    "${local.url_amplify}/",
  ]

  explicit_auth_flows = ["ALLOW_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH"]
  access_token_validity = 60
  id_token_validity     = 60
  token_validity_units {
    access_token = "minutes"
    id_token     = "minutes"
  }
}

# Declara que existen; no los concede al cliente SPA.
resource "aws_cognito_resource_server" "presupuestos" {
  user_pool_id = aws_cognito_user_pool.pool.id
  identifier   = "presupuestos"
  name         = "API de presupuestos"

  scope {
    scope_name        = "read"
    scope_description = "Listar y ver solicitudes"
  }
  scope {
    scope_name        = "write"
    scope_description = "Crear solicitudes de presupuesto"
  }
  scope {
    scope_name        = "decidir"
    scope_description = "Aprobar o rechazar solicitudes"
  }
}

resource "aws_cognito_user_group" "trabajadores" {
  user_pool_id = aws_cognito_user_pool.pool.id
  name         = "trabajadores"
  description  = "Puede solicitar presupuesto"
}

resource "aws_cognito_user_group" "administradores" {
  user_pool_id = aws_cognito_user_pool.pool.id
  name         = "administradores"
  description  = "Puede aprobar o rechazar solicitudes"
}
