# Provider 설정
provider "aws" {
  region = "ap-northeast-2"
}

# VPC 생성
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "portfolio-vpc"
  }
}

# 인터넷 게이트웨이
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "portfolio-igw"
  }
}

# Public 서브넷
resource "aws_subnet" "public_1" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.3.0/24"
  availability_zone       = "ap-northeast-2a"
  map_public_ip_on_launch = true

  tags = {
    Name = "portfolio-public-1"
  }
}

# 두 번째 Public 서브넷
resource "aws_subnet" "public_2" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.4.0/24"
  availability_zone       = "ap-northeast-2c"
  map_public_ip_on_launch = true

  tags = {
    Name = "portfolio-public-2"
  }
}

# Public 라우팅 테이블
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "portfolio-public-rt"
  }
}

# Public 서브넷 라우팅 연결
resource "aws_route_table_association" "public_1" {
  subnet_id      = aws_subnet.public_1.id
  route_table_id = aws_route_table.public.id
}

# Public 서브넷 라우팅 연결 (두 번째 서브넷용)
resource "aws_route_table_association" "public_2" {
  subnet_id      = aws_subnet.public_2.id
  route_table_id = aws_route_table.public.id
}

# 보안 그룹 - EC2 (공통으로 사용, 필요한 포트 추가)
resource "aws_security_group" "ec2" {
  name        = "portfolio-ec2-sg"
  description = "Security group for EC2"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    self        = true  # 같은 보안 그룹 인스턴스끼리 모든 포트 허용
    description = "Allow all internal traffic within the same SG"
  }

  # Spring Boot 메인 애플리케이션용 8080 포트
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow Spring Boot main application"
  }

  # Spring Boot 메일 애플리케이션용 8081 포트
  ingress {
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow Spring Boot mail application"
  }

  # SSH 접속
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Prometheus
  ingress {
    from_port   = 9090
    to_port     = 9090
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Grafana
  ingress {
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Redis
  ingress {
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Kafka
  ingress {
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # MailHog SMTP (1025) and Web UI (8025)
  ingress {
    from_port   = 1025
    to_port     = 1025
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "MailHog SMTP"
  }

  ingress {
    from_port   = 8025
    to_port     = 8025
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "MailHog Web UI"
  }

  # 아웃바운드 트래픽 허용
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "portfolio-ec2-sg"
  }
}

# IAM 역할 생성
resource "aws_iam_role" "ec2_role" {
  name = "portfolio-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

# SSM 관리형 정책 연결
resource "aws_iam_role_policy_attachment" "ssm_policy" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# ECR 정책 추가
resource "aws_iam_role_policy" "ecr_policy" {
  name = "ecr-policy"
  role = aws_iam_role.ec2_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage"
        ]
        Resource = "*"
      }
    ]
  })
}

# CloudWatch Agent 정책 연결
resource "aws_iam_role_policy_attachment" "cloudwatch_agent" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

# EC2용 인스턴스 프로파일 생성
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "portfolio-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# 키 페어 생성
resource "aws_key_pair" "portfolio" {
  key_name   = "portfolio-key"
  public_key = file("${path.module}/portfolio-key.pub")  # 로컬에 있는 public key 파일 경로
}

# EC2 인스턴스 - app-main (메인 백엔드 8080)
resource "aws_instance" "app_main" {
  ami                    = "ami-0e9bfdb247cc8de84"  # Ubuntu 22.04 LTS AMI
  instance_type          = "t2.micro"
  subnet_id              = aws_subnet.public_1.id
  monitoring             = true
  vpc_security_group_ids = [aws_security_group.ec2.id]
  key_name               = aws_key_pair.portfolio.key_name
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  user_data = <<-EOF
              #!/bin/bash
              set -e  # 오류 발생 시 스크립트 중단

              # SSM Agent 설치
              sudo snap install amazon-ssm-agent --classic
              sudo systemctl start snap.amazon-ssm-agent.amazon-ssm-agent.service
              sudo systemctl enable snap.amazon-ssm-agent.amazon-ssm-agent.service

              # AWS CLI 설치
              sudo apt-get update
              sudo apt-get install -y unzip
              curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
              unzip awscliv2.zip
              sudo ./aws/install

              # Docker 설치
              sudo apt-get install -y ca-certificates curl gnupg
              sudo install -m 0755 -d /etc/apt/keyrings
              curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
              sudo chmod a+r /etc/apt/keyrings/docker.gpg
              echo "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
              sudo apt-get update
              sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
              sudo usermod -aG docker ubuntu
              sudo systemctl start docker
              sudo systemctl enable docker

              # Docker Compose 설치
              sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
              sudo chmod +x /usr/local/bin/docker-compose

              # CloudWatch Agent 설치 및 설정
              sudo wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
              sudo dpkg -i amazon-cloudwatch-agent.deb

              # CloudWatch Agent 설정 (메모리 및 스왑 모니터링)
              sudo mkdir -p /opt/aws/amazon-cloudwatch-agent/etc/
              sudo bash -c 'cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json' << 'EOT'
              {
                "agent": {
                  "metrics_collection_interval": 60,
                  "run_as_user": "root"
                },
                "metrics": {
                  "append_dimensions": {
                    "InstanceId": "$${aws:InstanceId}",
                    "InstanceType": "$${aws:InstanceType}",
                    "AutoScalingGroupName": "$${aws:AutoScalingGroupName}"
                  },
                  "metrics_collected": {
                    "mem": {
                      "measurement": [
                        "mem_used_percent"
                      ],
                      "metrics_collection_interval": 60
                    },
                    "swap": {
                      "measurement": [
                        "swap_used_percent"
                      ]
                    }
                  }
                }
              }
              EOT

              # CloudWatch Agent 시작
              sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
              sudo systemctl start amazon-cloudwatch-agent
              sudo systemctl enable amazon-cloudwatch-agent

              # MailHog Docker로 띄우기
              docker run -d -p 1025:1025 -p 8025:8025 --name mailhog mailhog/mailhog
              EOF

  tags = {
    Name = "portfolio-app-main"
  }
}

# EC2 인스턴스 - app-mail (메일 백엔드 8081 + MailHog)
resource "aws_instance" "app_mail" {
  ami                    = "ami-0e9bfdb247cc8de84"  # Ubuntu 22.04 LTS AMI
  instance_type          = "t2.micro"
  subnet_id              = aws_subnet.public_2.id  # 다른 AZ로 분산
  monitoring             = true
  vpc_security_group_ids = [aws_security_group.ec2.id]
  key_name               = aws_key_pair.portfolio.key_name
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  user_data = <<-EOF
              #!/bin/bash
              set -e  # 오류 발생 시 스크립트 중단

              # SSM Agent 설치
              sudo snap install amazon-ssm-agent --classic
              sudo systemctl start snap.amazon-ssm-agent.amazon-ssm-agent.service
              sudo systemctl enable snap.amazon-ssm-agent.amazon-ssm-agent.service

              # AWS CLI 설치
              sudo apt-get update
              sudo apt-get install -y unzip
              curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
              unzip awscliv2.zip
              sudo ./aws/install

              # Docker 설치
              sudo apt-get install -y ca-certificates curl gnupg
              sudo install -m 0755 -d /etc/apt/keyrings
              curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
              sudo chmod a+r /etc/apt/keyrings/docker.gpg
              echo "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
              sudo apt-get update
              sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
              sudo usermod -aG docker ubuntu
              sudo systemctl start docker
              sudo systemctl enable docker

              # Docker Compose 설치
              sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
              sudo chmod +x /usr/local/bin/docker-compose

              # CloudWatch Agent 설치 및 설정
              sudo wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
              sudo dpkg -i amazon-cloudwatch-agent.deb

              # CloudWatch Agent 설정 (메모리 및 스왑 모니터링)
              sudo mkdir -p /opt/aws/amazon-cloudwatch-agent/etc/
              sudo bash -c 'cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json' << 'EOT'
              {
                "agent": {
                  "metrics_collection_interval": 60,
                  "run_as_user": "root"
                },
                "metrics": {
                  "append_dimensions": {
                    "InstanceId": "$${aws:InstanceId}",
                    "InstanceType": "$${aws:InstanceType}",
                    "AutoScalingGroupName": "$${aws:AutoScalingGroupName}"
                  },
                  "metrics_collected": {
                    "mem": {
                      "measurement": [
                        "mem_used_percent"
                      ],
                      "metrics_collection_interval": 60
                    },
                    "swap": {
                      "measurement": [
                        "swap_used_percent"
                      ]
                    }
                  }
                }
              }
              EOT

              # CloudWatch Agent 시작
              sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
              sudo systemctl start amazon-cloudwatch-agent
              sudo systemctl enable amazon-cloudwatch-agent

              # MailHog Docker로 띄우기
              docker run -d -p 1025:1025 -p 8025:8025 --name mailhog mailhog/mailhog
              EOF

  tags = {
    Name = "portfolio-app-mail"
  }
}

# EC2 인스턴스 - monitoring (Prometheus 9090 + Grafana 3000)
resource "aws_instance" "monitoring" {
  ami                    = "ami-0e9bfdb247cc8de84"  # Ubuntu 22.04 LTS AMI
  instance_type          = "t2.micro"
  subnet_id              = aws_subnet.public_1.id
  monitoring             = true
  vpc_security_group_ids = [aws_security_group.ec2.id]
  key_name               = aws_key_pair.portfolio.key_name
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  # Prometheus와 Grafana를 Docker로 띄우고, Prometheus config에서 app-main과 app-mail 모니터링 (private IP 사용)
  user_data = <<-EOF
              #!/bin/bash
              set -e  # 오류 발생 시 스크립트 중단

              # SSM Agent 설치
              sudo snap install amazon-ssm-agent --classic
              sudo systemctl start snap.amazon-ssm-agent.amazon-ssm-agent.service
              sudo systemctl enable snap.amazon-ssm-agent.amazon-ssm-agent.service

              # AWS CLI 설치
              sudo apt-get update
              sudo apt-get install -y unzip
              curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
              unzip awscliv2.zip
              sudo ./aws/install

              # Docker 설치
              sudo apt-get install -y ca-certificates curl gnupg
              sudo install -m 0755 -d /etc/apt/keyrings
              curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
              sudo chmod a+r /etc/apt/keyrings/docker.gpg
              echo "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
              sudo apt-get update
              sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
              sudo usermod -aG docker ubuntu
              sudo systemctl start docker
              sudo systemctl enable docker

              # Docker Compose 설치
              sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
              sudo chmod +x /usr/local/bin/docker-compose

              # CloudWatch Agent 설치 및 설정
              sudo wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
              sudo dpkg -i amazon-cloudwatch-agent.deb

              # CloudWatch Agent 설정 (메모리 및 스왑 모니터링)
              sudo mkdir -p /opt/aws/amazon-cloudwatch-agent/etc/
              sudo bash -c 'cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json' << 'EOT'
              {
                "agent": {
                  "metrics_collection_interval": 60,
                  "run_as_user": "root"
                },
                "metrics": {
                  "append_dimensions": {
                    "InstanceId": "$${aws:InstanceId}",
                    "InstanceType": "$${aws:InstanceType}",
                    "AutoScalingGroupName": "$${aws:AutoScalingGroupName}"
                  },
                  "metrics_collected": {
                    "mem": {
                      "measurement": [
                        "mem_used_percent"
                      ],
                      "metrics_collection_interval": 60
                    },
                    "swap": {
                      "measurement": [
                        "swap_used_percent"
                      ]
                    }
                  }
                }
              }
              EOT

              # CloudWatch Agent 시작
              sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
              sudo systemctl start amazon-cloudwatch-agent
              sudo systemctl enable amazon-cloudwatch-agent

              # Prometheus 설정 디렉토리 생성
              mkdir -p /etc/prometheus
              cat << EOC > /etc/prometheus/prometheus.yml
              global:
                scrape_interval: 15s
              scrape_configs:
                - job_name: 'app-main'
                  metrics_path: '/actuator/prometheus'
                  static_configs:
                    - targets: ['${aws_instance.app_main.private_ip}:8080']  # app-main private IP (메트릭 엔드포인트 가정)
                - job_name: 'app-mail'
                  metrics_path: '/actuator/prometheus'
                  static_configs:
                    - targets: ['${aws_instance.app_mail.private_ip}:8081']  # app-mail private IP (메트릭 엔드포인트 가정)
                - job_name: 'redis'
                  static_configs:
                    - targets: ['${aws_instance.redis.private_ip}:9121']
              EOC

              # Prometheus Docker로 띄우기
              docker run -d -p 9090:9090 --name prometheus -v /etc/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml prom/prometheus

              # Grafana Docker로 띄우기 (Prometheus datasource 수동 설정 필요)
              docker run -d -p 3000:3000 --name grafana grafana/grafana
              EOF

  tags = {
    Name = "portfolio-monitoring"
  }
}

# EC2 인스턴스 - redis (Redis + redis_exporter 자동 설치)
resource "aws_instance" "redis" {
  ami                    = "ami-0e9bfdb247cc8de84"  # Ubuntu 22.04 LTS
  instance_type          = "t2.micro"
  subnet_id              = aws_subnet.public_2.id
  monitoring             = true
  vpc_security_group_ids = [aws_security_group.ec2.id]
  key_name               = aws_key_pair.portfolio.key_name
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  user_data = <<-EOF
              #!/bin/bash
              set -e

              # 기존 SSM, AWS CLI, Docker, CloudWatch 설정 (생략 없이 그대로 복붙)

              # SSM Agent 설치
              sudo snap install amazon-ssm-agent --classic
              sudo systemctl start snap.amazon-ssm-agent.amazon-ssm-agent.service
              sudo systemctl enable snap.amazon-ssm-agent.amazon-ssm-agent.service

              # AWS CLI 설치
              sudo apt-get update
              sudo apt-get install -y unzip
              curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
              unzip awscliv2.zip
              sudo ./aws/install

              # Docker 설치
              sudo apt-get install -y ca-certificates curl gnupg
              sudo install -m 0755 -d /etc/apt/keyrings
              curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
              sudo chmod a+r /etc/apt/keyrings/docker.gpg
              echo "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
              sudo apt-get update
              sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
              sudo usermod -aG docker ubuntu
              sudo systemctl start docker
              sudo systemctl enable docker

              # Docker Compose 설치
              sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
              sudo chmod +x /usr/local/bin/docker-compose

              # CloudWatch Agent 설치 및 설정 (기존 그대로 유지)
              sudo wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
              sudo dpkg -i amazon-cloudwatch-agent.deb
              sudo mkdir -p /opt/aws/amazon-cloudwatch-agent/etc/
              sudo bash -c 'cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json' << 'EOT'
              {
                "agent": {
                  "metrics_collection_interval": 60,
                  "run_as_user": "root"
                },
                "metrics": {
                  "append_dimensions": {
                    "InstanceId": "$${aws:InstanceId}",
                    "InstanceType": "$${aws:InstanceType}"
                  },
                  "metrics_collected": {
                    "mem": { "measurement": ["mem_used_percent"] },
                    "swap": { "measurement": ["swap_used_percent"] }
                  }
                }
              }
              EOT
              sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
              sudo systemctl start amazon-cloudwatch-agent
              sudo systemctl enable amazon-cloudwatch-agent

              # Redis 본체 실행
              docker run -d --name redis \
                -p 6379:6379 \
                --restart unless-stopped \
                redis:7-alpine

              # Redis Exporter 실행 (Prometheus가 Redis 메트릭 수집 가능하게!)
              docker run -d --name redis-exporter \
                -p 9121:9121 \
                --restart unless-stopped \
                -e REDIS_ADDR=redis://localhost:6379 \
                oliver006/redis_exporter:latest \
                --redis.addr=redis://localhost:6379 \
                --web.listen-address=:9121

              EOF

  tags = {
    Name = "portfolio-redis"
  }
}

# EC2 인스턴스 - kafka (Kafka 9092 + Zookeeper)
resource "aws_instance" "kafka" {
  ami                    = "ami-0e9bfdb247cc8de84"  # Ubuntu 22.04 LTS AMI
  instance_type          = "t3.small"
  subnet_id              = aws_subnet.public_1.id
  monitoring             = true
  vpc_security_group_ids = [aws_security_group.ec2.id]
  key_name               = aws_key_pair.portfolio.key_name
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  user_data = <<-EOF
              #!/bin/bash
              set -e  # 오류 발생 시 스크립트 중단

              # SSM Agent 설치
              sudo snap install amazon-ssm-agent --classic
              sudo systemctl start snap.amazon-ssm-agent.amazon-ssm-agent.service
              sudo systemctl enable snap.amazon-ssm-agent.amazon-ssm-agent.service

              # AWS CLI 설치
              sudo apt-get update
              sudo apt-get install -y unzip
              curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
              unzip awscliv2.zip
              sudo ./aws/install

              # Docker 설치
              sudo apt-get install -y ca-certificates curl gnupg
              sudo install -m 0755 -d /etc/apt/keyrings
              curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
              sudo chmod a+r /etc/apt/keyrings/docker.gpg
              echo "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
              sudo apt-get update
              sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
              sudo usermod -aG docker ubuntu
              sudo systemctl start docker
              sudo systemctl enable docker

              # Docker Compose 설치
              sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
              sudo chmod +x /usr/local/bin/docker-compose

              # CloudWatch Agent 설치 및 설정
              sudo wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
              sudo dpkg -i amazon-cloudwatch-agent.deb

              # CloudWatch Agent 설정 (메모리 및 스왑 모니터링)
              sudo mkdir -p /opt/aws/amazon-cloudwatch-agent/etc/
              sudo bash -c 'cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json' << 'EOT'
              {
                "agent": {
                  "metrics_collection_interval": 60,
                  "run_as_user": "root"
                },
                "metrics": {
                  "append_dimensions": {
                    "InstanceId": "$${aws:InstanceId}",
                    "InstanceType": "$${aws:InstanceType}",
                    "AutoScalingGroupName": "$${aws:AutoScalingGroupName}"
                  },
                  "metrics_collected": {
                    "mem": {
                      "measurement": [
                        "mem_used_percent"
                      ],
                      "metrics_collection_interval": 60
                    },
                    "swap": {
                      "measurement": [
                        "swap_used_percent"
                      ]
                    }
                  }
                }
              }
              EOT

              # CloudWatch Agent 시작
              sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
              sudo systemctl start amazon-cloudwatch-agent
              sudo systemctl enable amazon-cloudwatch-agent

              # Kafka Docker로 띄우기
              docker run -d --name kafka \
                              -p 9092:9092 \
                              --restart unless-stopped \
                              -e KAFKA_NODE_ID=1 \
                              -e KAFKA_PROCESS_ROLES='broker,controller' \
                              -e KAFKA_LISTENERS='PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093' \
                              -e KAFKA_ADVERTISED_LISTENERS='PLAINTEXT://localhost:9092' \
                              -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP='PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT' \
                              -e KAFKA_INTER_BROKER_LISTENER_NAME='PLAINTEXT' \
                              -e KAFKA_CONTROLLER_LISTENER_NAMES='CONTROLLER' \
                              -e KAFKA_CONTROLLER_QUORUM_VOTERS='1@localhost:9093' \
                              -e KAFKA_NUM_PARTITIONS=2 \                    # ← 여기 추가! 기본 파티션 10개
                              -e KAFKA_DEFAULT_REPLICATION_FACTOR=1 \         # 단일 노드라 복제 1
                              -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
                              -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
                              -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
                              -e KAFKA_AUTO_CREATE_TOPICS_ENABLE='true' \
                              apache/kafka:4.0.0
              EOF

  tags = {
    Name = "portfolio-kafka"
  }
}

# 탄력적 IP (app-main에 할당, 필요시 다른 인스턴스에도 추가)
resource "aws_eip" "app_main" {
  instance = aws_instance.app_main.id

  tags = {
    Name = "portfolio-app-main-eip"
  }
}

# RDS 보안 그룹
resource "aws_security_group" "rds" {
  name        = "portfolio-rds-sg"
  description = "Security group for RDS"
  vpc_id      = aws_vpc.main.id

  # 외부에서의 MySQL 접속 허용
  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    cidr_blocks     = ["0.0.0.0/0"]  # 모든 IP에서 접근 가능
  }

  tags = {
    Name = "portfolio-rds-sg"
  }
}

# RDS 서브넷 그룹
resource "aws_db_subnet_group" "rds" {
  name       = "portfolio-rds-subnet-group"
  subnet_ids = [aws_subnet.public_1.id, aws_subnet.public_2.id]

  tags = {
    Name = "portfolio-rds-subnet-group"
  }
}

# RDS 향상된 모니터링을 위한 IAM 역할
resource "aws_iam_role" "rds_enhanced_monitoring" {
  name = "portfolio-rds-enhanced-monitoring"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "monitoring.rds.amazonaws.com"
        }
      }
    ]
  })
}

# 향상된 모니터링 정책 연결
resource "aws_iam_role_policy_attachment" "rds_enhanced_monitoring" {
  role       = aws_iam_role.rds_enhanced_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

# RDS 인스턴스
resource "aws_db_instance" "portfolio" {
  identifier             = "portfolio-db"
  engine                 = "mysql"
  engine_version         = "8.0"
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  storage_type           = "gp2"

  db_name                = "portfolio"
  username               = "portfolio_user"
  password               = var.db_password

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.rds.name

  skip_final_snapshot    = true
  publicly_accessible    = true

  monitoring_interval    = 60  # 60초마다 지표 수집
  monitoring_role_arn    = aws_iam_role.rds_enhanced_monitoring.arn

  tags = {
    Name = "portfolio-db"
  }
}

# ECR 저장소 생성
resource "aws_ecr_repository" "app" {
  name         = "backend-portfolio"
  force_delete = true

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "backend-portfolio"
  }
}

# ECR 저장소 정책 (선택사항)
resource "aws_ecr_repository_policy" "app_policy" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowPushPull"
        Effect = "Allow"
        Principal = {
          AWS = "*"
        }
        Action = [
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:BatchCheckLayerAvailability",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload"
        ]
      }
    ]
  })
}

# CloudWatch 대시보드 생성 (여러 인스턴스에 대한 위젯 추가)
resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "portfolio-dashboard"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          view    = "timeSeries"
          stacked = false
          metrics = [
            ["AWS/EC2", "CPUUtilization", "InstanceId", aws_instance.app_main.id],
            [".", ".", ".", aws_instance.app_mail.id]
          ]
          region = "ap-northeast-2"
          title  = "EC2 백엔드 CPU 사용률 (%)"
          period = 300
          stat   = "Average"
          yAxis = {
            left = {
              min = 0
              max = 100
            }
          }
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          view    = "timeSeries"
          stacked = false
          metrics = [
            [
              "CWAgent",
              "mem_used_percent",
              "InstanceId", aws_instance.app_main.id,
              "InstanceType", aws_instance.app_main.instance_type
            ],
            [
              ".",
              ".",
              "InstanceId", aws_instance.app_mail.id,
              "InstanceType", aws_instance.app_mail.instance_type
            ]
          ]
          region = "ap-northeast-2"
          title  = "EC2 백엔드 메모리 사용률 (%)"
          period = 300
          stat   = "Average"
          yAxis = {
            left = {
              min = 0
              max = 100
            }
          }
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 12
        height = 6
        properties = {
          view    = "timeSeries"
          stacked = false
          metrics = [
            ["AWS/EC2", "CPUUtilization", "InstanceId", aws_instance.monitoring.id],
            [".", ".", ".", aws_instance.redis.id],
            [".", ".", ".", aws_instance.kafka.id]
          ]
          region = "ap-northeast-2"
          title  = "기타 EC2 CPU 사용률 (%)"
          period = 300
          stat   = "Average"
          yAxis = {
            left = {
              min = 0
              max = 100
            }
          }
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 6
        width  = 12
        height = 6
        properties = {
          view    = "timeSeries"
          stacked = false
          metrics = [
            [
              "CWAgent",
              "mem_used_percent",
              "InstanceId", aws_instance.monitoring.id,
              "InstanceType", aws_instance.monitoring.instance_type
            ],
            [
              ".",
              ".",
              "InstanceId", aws_instance.redis.id,
              "InstanceType", aws_instance.redis.instance_type
            ],
            [
              ".",
              ".",
              "InstanceId", aws_instance.kafka.id,
              "InstanceType", aws_instance.kafka.instance_type
            ]
          ]
          region = "ap-northeast-2"
          title  = "기타 EC2 메모리 사용률 (%)"
          period = 300
          stat   = "Average"
          yAxis = {
            left = {
              min = 0
              max = 100
            }
          }
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 12
        width  = 12
        height = 6
        properties = {
          view    = "timeSeries"
          stacked = false
          metrics = [
            ["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", aws_db_instance.portfolio.identifier]
          ]
          region = "ap-northeast-2"
          title  = "RDS CPU 사용률 (%)"
          period = 300
          stat   = "Average"
          yAxis = {
            left = {
              min = 0
              max = 100
            }
          }
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 12
        width  = 12
        height = 6
        properties = {
          view    = "timeSeries"
          stacked = false
          metrics = [
            ["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", aws_db_instance.portfolio.identifier],
            ["AWS/RDS", "FreeableMemory", "DBInstanceIdentifier", aws_db_instance.portfolio.identifier],
            ["AWS/RDS", "FreeStorageSpace", "DBInstanceIdentifier", aws_db_instance.portfolio.identifier]
          ]
          region = "ap-northeast-2"
          title  = "RDS 상태"
          period = 300
          stat   = "Average"
        }
      }
    ]
  })
}

# 출력값
output "app_main_public_ip" {
  value = aws_eip.app_main.public_ip
}

output "app_main_private_ip" {
  value = aws_instance.app_main.private_ip
}

output "app_mail_private_ip" {
  value = aws_instance.app_mail.private_ip  # Spring mail host로 사용
}

output "app_mail_public_ip" {
  value = aws_instance.app_mail.public_ip  # MailHog UI 접근용 (필요시)
}

output "monitoring_public_ip" {
  value = aws_instance.monitoring.public_ip
}

output "redis_public_ip" {
  value = aws_instance.redis.public_ip
}

output "kafka_public_ip" {
  value = aws_instance.kafka.public_ip
}

output "ssh_command_app_main" {
  value = "ssh -i portfolio-key.pem ubuntu@${aws_eip.app_main.public_ip}"
}

output "instance_ids" {
  value = {
    app_main    = aws_instance.app_main.id
    app_mail    = aws_instance.app_mail.id
    monitoring  = aws_instance.monitoring.id
    redis       = aws_instance.redis.id
    kafka       = aws_instance.kafka.id
  }
  description = "EC2 인스턴스 ID들"
}

output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "rds_endpoint" {
  value = aws_db_instance.portfolio.endpoint
}

output "redis_private_ip" {
  value       = aws_instance.redis.private_ip
  description = "Redis private IP (application.yml에서 사용)"
}

output "kafka_private_ip" {
  value       = aws_instance.kafka.private_ip
  description = "Kafka private IP (bootstrap-servers에 사용)"
}