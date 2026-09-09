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

output "url_datos_protegido" {
  description = "Ruta protegida. Sin Bearer responde 401."
  value       = "${aws_apigatewayv2_api.api_manager.api_endpoint}/datos"
}

output "url_datos_publico" {
  value = "${aws_apigatewayv2_api.api_manager.api_endpoint}/publico/datos"
}

output "url_productos" {
  value = "${aws_apigatewayv2_api.api_manager.api_endpoint}/productos"
}

output "probar_sin_token" {
  description = "Debe responder 401."
  value       = "curl -s -o /dev/null -w 'HTTP %%{http_code}\\n' ${aws_apigatewayv2_api.api_manager.api_endpoint}/datos"
}

# Lo lee scripts/publicar-ecs.sh
output "api_id" {
  value = aws_apigatewayv2_api.api_manager.id
}

output "integracion_id" {
  value = aws_apigatewayv2_integration.backend.id
}

output "integracion_productos_coleccion_id" {
  value = aws_apigatewayv2_integration.productos_coleccion.id
}

output "integracion_productos_elemento_id" {
  value = aws_apigatewayv2_integration.productos_elemento.id
}
