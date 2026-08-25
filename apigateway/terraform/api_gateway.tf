resource "aws_apigatewayv2_api" "api_manager" {
  name          = "api-mindicador"
  protocol_type = "HTTP"

  cors_configuration {
    allow_headers = ["authorization", "content-type"]
    allow_methods = ["GET", "OPTIONS"]
    allow_origins = ["http://localhost:5173"]
    max_age       = 300
  }
}

