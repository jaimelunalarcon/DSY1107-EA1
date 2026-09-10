# =============================================================================
# Lambda Pre Token Generation V2: grupos Cognito -> scopes del access token.
# =============================================================================

data "archive_file" "user_token_ms" {
  type        = "zip"
  source_file = "${path.module}/../user-token-ms/index.mjs"
  output_path = "${path.module}/user-token-ms.zip"
}

resource "aws_lambda_function" "user_token_ms" {
  function_name = "user-token-ms-${var.estudiante}"
  description   = "Pre token generation V2: agrega scopes segun el grupo del usuario"
  role          = "arn:aws:iam::${data.aws_caller_identity.actual.account_id}:role/LabRole"
  runtime       = "nodejs22.x"
  handler       = "index.handler"
  filename         = data.archive_file.user_token_ms.output_path
  source_code_hash = data.archive_file.user_token_ms.output_base64sha256
  timeout     = 5
  memory_size = 128
}

resource "aws_lambda_permission" "cognito" {
  statement_id  = "AllowCognitoInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.user_token_ms.function_name
  principal     = "cognito-idp.amazonaws.com"
  source_arn    = aws_cognito_user_pool.pool.arn
}

resource "aws_cloudwatch_log_group" "user_token_ms" {
  name              = "/aws/lambda/${aws_lambda_function.user_token_ms.function_name}"
  retention_in_days = 7
}

output "lambda_user_token_ms" {
  value = aws_lambda_function.user_token_ms.function_name
}

output "lambda_user_token_ms_logs" {
  value = "aws logs tail ${aws_cloudwatch_log_group.user_token_ms.name} --follow"
}
