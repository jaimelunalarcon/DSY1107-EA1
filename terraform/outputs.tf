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

output "aws_region" {
  value = data.aws_region.current.name
}

output "url_presupuestos" {
  value = "${aws_apigatewayv2_api.api_manager.api_endpoint}/presupuestos"
}

output "probar_sin_token" {
  description = "Debe responder 401."
  value       = "curl -s -o /dev/null -w 'HTTP %%{http_code}\\n' ${aws_apigatewayv2_api.api_manager.api_endpoint}/presupuestos"
}

# Lo lee scripts/publicar-ecs.sh
output "api_id" {
  value = aws_apigatewayv2_api.api_manager.id
}

output "integracion_presupuestos_coleccion_id" {
  value = aws_apigatewayv2_integration.presupuestos_coleccion.id
}

output "integracion_presupuestos_elemento_id" {
  value = aws_apigatewayv2_integration.presupuestos_elemento.id
}
