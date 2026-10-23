# =============================================================================
# Microservicios EP2 en el mismo cluster ECS que el backend:
#   - notificaciones-ms (consumidores ACK + DLQ) :8081
#   - rabbit-admin-ms (REST admin)               :8082
#
# Apuntan al RabbitMQ de rabbitmq_ec2.tf. Orden:
#   1. terraform apply   (EC2 + repos ECR + servicios)
#   2. ./scripts/publicar-ms-ecs.sh notificaciones-ms
#   3. ./scripts/publicar-ms-ecs.sh rabbit-admin-ms
# =============================================================================

resource "aws_ecr_repository" "notificaciones" {
  name                 = "dsy1107-notificaciones-${var.estudiante}"
  force_delete         = true
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = false
  }
}

resource "aws_ecr_repository" "rabbit_admin" {
  name                 = "dsy1107-rabbit-admin-${var.estudiante}"
  force_delete         = true
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = false
  }
}

resource "aws_cloudwatch_log_group" "notificaciones" {
  name              = "/ecs/dsy1107-notificaciones-${var.estudiante}"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "rabbit_admin" {
  name              = "/ecs/dsy1107-rabbit-admin-${var.estudiante}"
  retention_in_days = 7
}

resource "aws_ecs_task_definition" "notificaciones" {
  family                   = "dsy1107-notificaciones-${var.estudiante}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = "arn:aws:iam::${data.aws_caller_identity.actual.account_id}:role/LabRole"

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name      = "notificaciones"
      image     = "${aws_ecr_repository.notificaciones.repository_url}:latest"
      essential = true
      portMappings = [
        { containerPort = 8081, protocol = "tcp" }
      ]
      environment = [
        { name = "SERVER_PORT", value = "8081" },
        { name = "RABBITMQ_ADDRESSES", value = local.rabbitmq_addresses },
        { name = "RABBITMQ_USER", value = var.rabbitmq_user },
        { name = "RABBITMQ_PASSWORD", value = var.rabbitmq_password }
      ]
      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:8081/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 90
      }
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.notificaciones.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "notificaciones"
        }
      }
    }
  ])
}

resource "aws_ecs_task_definition" "rabbit_admin" {
  family                   = "dsy1107-rabbit-admin-${var.estudiante}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = "arn:aws:iam::${data.aws_caller_identity.actual.account_id}:role/LabRole"

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name      = "rabbit-admin"
      image     = "${aws_ecr_repository.rabbit_admin.repository_url}:latest"
      essential = true
      portMappings = [
        { containerPort = 8082, protocol = "tcp" }
      ]
      environment = [
        { name = "SERVER_PORT", value = "8082" },
        { name = "RABBITMQ_ADDRESSES", value = local.rabbitmq_addresses },
        { name = "RABBITMQ_USER", value = var.rabbitmq_user },
        { name = "RABBITMQ_PASSWORD", value = var.rabbitmq_password }
      ]
      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:8082/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.rabbit_admin.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "rabbit-admin"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "notificaciones" {
  name            = "dsy1107-notificaciones-${var.estudiante}"
  cluster         = aws_ecs_cluster.backend.id
  task_definition = aws_ecs_task_definition.notificaciones.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.publicas.ids
    security_groups  = [aws_security_group.tarea.id]
    assign_public_ip = true
  }

  wait_for_steady_state = false
}

resource "aws_ecs_service" "rabbit_admin" {
  name            = "dsy1107-rabbit-admin-${var.estudiante}"
  cluster         = aws_ecs_cluster.backend.id
  task_definition = aws_ecs_task_definition.rabbit_admin.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.publicas.ids
    security_groups  = [aws_security_group.tarea.id]
    assign_public_ip = true
  }

  wait_for_steady_state = false
}

output "ecr_notificaciones" {
  value = aws_ecr_repository.notificaciones.repository_url
}

output "ecr_rabbit_admin" {
  value = aws_ecr_repository.rabbit_admin.repository_url
}

output "ecs_servicio_notificaciones" {
  value = aws_ecs_service.notificaciones.name
}

output "ecs_servicio_rabbit_admin" {
  value = aws_ecs_service.rabbit_admin.name
}
