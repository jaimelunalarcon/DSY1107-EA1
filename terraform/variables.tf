variable "estudiante" {
  type        = string
  description = "Identificador único del alumno o grupo"
  default     = "grupo33"

  validation {
    condition     = can(regex("^[a-z0-9-]{3,20}$", var.estudiante))
    error_message = "Solo minúsculas, números y guiones, entre 3 y 20 caracteres."
  }
}

variable "aws_region" {
  type        = string
  description = "Región AWS"
  default     = "us-east-1"
}

variable "backend_url" {
  type        = string
  description = "URI inicial del HTTP_PROXY (mindicador). publicar-ecs.sh la reemplaza por la IP de la task."
  default     = "https://mindicador.cl/api"
}
