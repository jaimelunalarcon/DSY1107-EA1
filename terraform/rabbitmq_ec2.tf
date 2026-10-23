# =============================================================================
# RabbitMQ en EC2 + Docker (cluster 3 nodos, igual que docker/rabbitmq local).
#
# El Learner Lab no da Amazon MQ ni roles IAM nuevos: se usa LabInstanceProfile
# y una AMI Amazon Linux 2023. user_data instala Docker y levanta el compose.
#
# Backend / notificaciones-ms / rabbit-admin-ms (ECS) apuntan a:
#   ${private_ip}:5672,${private_ip}:5673,${private_ip}:5674
# =============================================================================

variable "rabbitmq_instancia" {
  description = "Tipo EC2 para el cluster RabbitMQ. t3.small puede ir justo; t3.medium es mas comodo con 3 nodos."
  type        = string
  default     = "t3.small"
}

variable "rabbitmq_user" {
  type    = string
  default = "admin"
}

variable "rabbitmq_password" {
  type      = string
  default   = "admin123"
  sensitive = true
}

data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

data "aws_iam_instance_profile" "lab" {
  name = "LabInstanceProfile"
}

resource "aws_security_group" "rabbitmq" {
  name        = "dsy1107-rabbitmq-${var.estudiante}"
  description = "AMQP desde tasks ECS; Management UI 15672 para la demo"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "AMQP nodos 1-3 desde el SG de las tasks ECS"
    from_port       = 5672
    to_port         = 5674
    protocol        = "tcp"
    security_groups = [aws_security_group.tarea.id]
  }

  ingress {
    description = "Management UI (demo EP2). Cerrar en produccion."
    from_port   = 15672
    to_port     = 15674
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "SSH opcional (troubleshooting)"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "dsy1107-rabbitmq-${var.estudiante}"
  }
}

resource "aws_instance" "rabbitmq" {
  ami                    = data.aws_ssm_parameter.al2023.value
  instance_type          = var.rabbitmq_instancia
  subnet_id              = data.aws_subnets.publicas.ids[0]
  vpc_security_group_ids = [aws_security_group.rabbitmq.id]
  iam_instance_profile   = data.aws_iam_instance_profile.lab.name

  # Misma VPC que ECS/RDS: las tasks llegan por IP privada.
  associate_public_ip_address = true

  user_data = base64encode(templatefile("${path.module}/rabbitmq-user-data.sh.tftpl", {
    rabbitmq_user     = var.rabbitmq_user
    rabbitmq_password = var.rabbitmq_password
    erlang_cookie     = "dsy1107_ep2_${var.estudiante}"
  }))

  user_data_replace_on_change = true

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  tags = {
    Name = "dsy1107-rabbitmq-${var.estudiante}"
  }
}

locals {
  rabbitmq_addresses = join(",", [
    "${aws_instance.rabbitmq.private_ip}:5672",
    "${aws_instance.rabbitmq.private_ip}:5673",
    "${aws_instance.rabbitmq.private_ip}:5674",
  ])
}

output "rabbitmq_private_ip" {
  description = "IP privada de la EC2 (la usan las tasks ECS)."
  value       = aws_instance.rabbitmq.private_ip
}

output "rabbitmq_public_ip" {
  description = "IP publica: Management UI http://IP:15672"
  value       = aws_instance.rabbitmq.public_ip
}

output "rabbitmq_addresses" {
  description = "spring.rabbitmq.addresses para backend / notificaciones / admin"
  value       = local.rabbitmq_addresses
}

output "rabbitmq_management_url" {
  value = "http://${aws_instance.rabbitmq.public_ip}:15672"
}

output "rabbitmq_user" {
  value = var.rabbitmq_user
}
