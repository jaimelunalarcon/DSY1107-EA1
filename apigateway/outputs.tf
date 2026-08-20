output "api_base_url" {
  description = "URL base del API Gateway HTTP"
  value       = aws_apigatewayv2_api.api_manager.api_endpoint
}

output "cognito_user_pool_id" {
  value = aws_cognito_user_pool.pool.id
}

output "cognito_client_id" {
  value = aws_cognito_user_pool_client.spa.id
}

output "cognito_domain" {
  value = "${aws_cognito_user_pool_domain.hosted_ui.domain}.auth.${data.aws_region.current.name}.amazoncognito.com"
}
