resource "aws_apigatewayv2_api" "api_manager" {
  name          = "api-presupuestos-${var.estudiante}"
  protocol_type = "HTTP"

  cors_configuration {
    # Amplify va SIN barra final (Origin nunca la lleva).
    allow_origins = [
      "http://localhost:5173",
      local.url_amplify,
    ]
    allow_methods = ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
    allow_headers = ["authorization", "content-type"]
    max_age       = 300
  }
}
