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
  description = "URI base inicial del HTTP_PROXY (placeholder). publicar-ecs.sh la reemplaza por http://IP:8080."
  default     = "http://127.0.0.1:8080"
}

variable "crear_ruta_internet" {
  type        = bool
  description = "Crear 0.0.0.0/0 al IGW en la route table principal. Pon false si el lab ya la tiene (RouteAlreadyExists)."
  default     = true
}
