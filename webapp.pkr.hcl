packer {
  required_plugins {
    amazon = {
      source  = "github.com/hashicorp/amazon"
      version = ">= 0.0.1, < 2.0.0"
    }

    # googlecompute = {
    #   source  = "github.com/hashicorp/googlecompute"
    #   version = ">= 1.0.0"
    # }
  }
}

# Variables
variable "aws_region" {
  type    = string
  default = "us-east-2"
}

# variable "gcp_project_id" {
#   type    = string
#   default = "csye6225-dev-451900"
# }

# variable "gcp_zone" {
#   type    = string
#   default = "us-east1-b"
# }

variable "aws_source_ami" {
  default = "ami-04ffc9f7871904759"
}

variable "DB_USER" {
  type    = string
  default = "root"
}

variable "DB_PASS" {
  type    = string
  default = "root"
}

variable "DB_NAME" {
  type    = string
  default = "healthcheck_db"
}

variable "DB_HOST" {
  type    = string
  default = "postgres-db-instance.c5ukym0ay3rj.us-east-2.rds.amazonaws.com"
}

variable "DB_PORT" {
  type    = string
  default = "5432"
}

variable "subnet_id" {
  type    = string
  default = "subnet-0d6a3e7640f0fb56f"
}

variable "AWS_REGION" {
  type    = string
  default = "us-east-2"
}

variable "S3_BUCKET_NAME" {
  type    = string
  default = "first-s3-bucket"
}

# Build images for AWS and GCP
source "amazon-ebs" "ubuntu" {
  ami_name      = "csye6225-${formatdate("YYYY-MM-DD_HH_mm_ss", timestamp())}"
  instance_type = "t2.micro"
  region        = var.aws_region
  source_ami    = "ami-0cb91c7de36eed2cb"
  vpc_filter {
    filters = {
      is-default = "true"
    }
  }
  subnet_id = var.subnet_id
  # source_ami_filter {
  #   filters = {
  #     name                = "ubuntu/images/*ubuntu-jammy-22.04-amd64-server-*"
  #     root-device-type    = "ebs"
  #     virtualization-type = "hvm"
  #   }
  #   most_recent = true
  #   owners      = ["099720109477"]
  # }
  ssh_username = "ubuntu"
  ssh_timeout  = "30m" # Increase SSH timeout
  tags = {
    Name = "health-check-api"
  }
}

# source "googlecompute" "ubuntu" {
#   project_id             = var.gcp_project_id
#   source_image_family    = "ubuntu-2204-lts"
#   machine_type           = "e2-micro"
#   zone                   = var.gcp_zone
#   image_name             = "csye6225-${formatdate("YYYY-MM-DD-HH-mm-ss", timestamp())}"
#   ssh_username           = "ubuntu"
#   ssh_timeout            = "30m"
#   ssh_handshake_attempts = 20
# }

build {
  name = "user-creation-testing"
  sources = [
    "source.amazon-ebs.ubuntu",
    # "source.googlecompute.ubuntu"
  ]

  # Provisioner to install dependencies
  provisioner "shell" {
    # environment_vars = [
    #   "DB_USER=${var.db_user}",
    #   "DB_PASS=${var.db_pass}"
    # ]
    inline = [
      "export DEBIAN_FRONTEND=noninteractive",
      "sudo apt-get update -y && sudo apt-get upgrade -y",
      # "sudo apt-get install -y software-properties-common", # Required for add-apt-repository
      # Add PostgreSQL repository and key
      # "sudo apt-get install -y wget ca-certificates",                                                                                      # Install wget and certificates
      # "wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo tee /etc/apt/trusted.gpg.d/postgresql.asc > /dev/null", # Use tee to output to file
      # "sudo apt-get update -y",
      "sudo apt-get install -y openjdk-17-jdk maven postgresql-client", # Install Java, Maven, and PostgreSQL client

      #Ensure that the PostgreSQL service is started and enabled
      # "sudo systemctl start postgresql",
      # "sudo systemctl enable postgresql",

      #Create the database and user for the application
      # "sudo -u postgres psql -c \"CREATE DATABASE healthcheck_db;\"",
      # "sudo -u postgres psql -c \"CREATE USER $DB_USER WITH PASSWORD '$DB_PASS';\"",
      # "sudo -u postgres psql -c \"GRANT ALL PRIVILEGES ON DATABASE healthcheck_db TO $DB_USER;\"",

      #Create a group and user for the application
      "sudo groupadd csye6225",
      "sudo useradd -g csye6225 -s /usr/sbin/nologin csye6225",

      # Create application directory
      "sudo mkdir -p /opt/csye6225",

      # Set ownership and permissions
      "sudo chown -R csye6225:csye6225 /opt/csye6225",
      "sudo chmod -R 750 /opt/csye6225",

      # Export environment variables for .env file creation
      "export DB_HOST=${var.DB_HOST}",
      "export DB_PORT=${var.DB_PORT}",
      "export DB_NAME=${var.DB_NAME}",

      # Create .env file
      "sudo touch /opt/csye6225/.env",
      "echo 'DB_URL=jdbc:postgresql://${var.DB_HOST}:${var.DB_PORT}/${var.DB_NAME}' | sudo tee -a /opt/csye6225/.env",
      "echo 'DB_USERNAME=root' | sudo tee -a /opt/csye6225/.env",
      "echo 'DB_PASSWORD=root' | sudo tee -a /opt/csye6225/.env",

      # Set ownership and permissions for .env file
      "sudo chown csye6225:csye6225 /opt/csye6225/.env",
      "sudo chmod 600 /opt/csye6225/.env",

    ]
  }

  provisioner "shell" {
    inline = [
      "export DB_HOST=${var.DB_HOST}",
      "export DB_PORT=${var.DB_PORT}",
      "export DB_NAME=${var.DB_NAME}",
      "export DB_USERNAME=${var.DB_USER}",
      "export DB_PASSWORD=${var.DB_PASS}",
      "export AWS_REGION=${var.AWS_REGION}",
      "export S3_BUCKET_NAME=${var.S3_BUCKET_NAME}",
      "sudo touch /opt/csye6225/application.properties",
      "echo 'server.port=8080' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'spring.datasource.url=jdbc:postgresql://${var.DB_HOST}:${var.DB_PORT}/${var.DB_NAME}' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'spring.datasource.username=${var.DB_USER}' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'spring.datasource.password=${var.DB_PASS}' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'spring.datasource.driver-class-name=org.postgresql.Driver' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'spring.jpa.hibernate.ddl-auto=update' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'spring.datasource.hikari.maximum-pool-size=5' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'spring.datasource.hikari.connection-timeout=30000' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'spring.datasource.hikari.idle-timeout=600000' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'spring.datasource.hikari.max-lifetime=7200000' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'logging.level.root=INFO' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'logging.level.org.hibernate.SQL=DEBUG' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'spring.sql.init.mode=always' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'spring.jpa.show-sql=false' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'aws.region=${var.AWS_REGION}' | sudo tee -a /opt/csye6225/application.properties",
      "echo 'aws.s3.bucket-name=${var.S3_BUCKET_NAME}' | sudo tee -a /opt/csye6225/application.properties",
      "sudo chown csye6225:csye6225 /opt/csye6225/application.properties",
      "sudo chmod 600 /opt/csye6225/application.properties"
    ]
  }

  provisioner "shell" {
    inline = [
      "sudo mkdir -p /tmp/app",
      "sudo chown ubuntu:ubuntu /tmp/app"
    ]
  }

  provisioner "file" {
    source      = "src"
    destination = "/tmp/app/src"
  }

  provisioner "file" {
    source      = "pom.xml"
    destination = "/tmp/app/pom.xml"
  }

  # provisioner "shell" {
  #   inline = [
  #     "cd /tmp/app",
  #     "mvn clean package",
  #     "mkdir -p target",
  #     "mv target/*.jar /tmp/app/target/"
  #   ]
  # }

  provisioner "shell" {
    inline = [
      "cd /tmp/app",
      "mvn clean package -DskipTests",
      "sudo mv target/health-check-api-0.0.1-SNAPSHOT.jar /opt/csye6225/application.jar",
      "sudo chown csye6225:csye6225 /opt/csye6225/application.jar",
      "sudo chmod 750 /opt/csye6225/application.jar",
    ]
  }

  # # Upload the application jar file to the instance
  # provisioner "file" {
  #   source      = "target/health-check-api-0.0.1-SNAPSHOT.jar"
  #   destination = "/tmp/application.jar"
  # }

  # Upload the .service file to the instance
  provisioner "file" {
    source      = "csye6225.service"
    destination = "/tmp/csye6225.service"
  }

  # Shell provisioner to move files and configure systemd
  provisioner "shell" {
    inline = [
      # Move files
      # "sudo mv /tmp/application.jar /opt/csye6225/",
      "sudo mv /tmp/app /opt/csye6225",
      "sudo mv /tmp/csye6225.service /etc/systemd/system/",

      # Set ownership and permissions
      # "sudo chown csye6225:csye6225 /opt/csye6225/application.jar",
      # "sudo chmod 750 /opt/csye6225/application.jar",
      "sudo chown -R csye6225:csye6225 /opt/csye6225",
      "sudo chmod -R 750 /opt/csye6225",
      "sudo chown root:root /etc/systemd/system/csye6225.service",
      "sudo chmod 644 /etc/systemd/system/csye6225.service",


      # Systemd configuration
      "sudo systemctl daemon-reload",
      "sudo systemctl enable csye6225.service",
      "sudo systemctl start csye6225.service",
    ]
  }

  #   provisioner "shell" {
  #     inline = [
  #       "echo 'DB_NAME=${var.DB_NAME}' | sudo tee -a /etc/environment",
  #       "echo 'DB_USER=${var.DB_USER}' | sudo tee -a /etc/environment",
  #       "echo 'DB_PASS=${var.DB_PASS}' | sudo tee -a /etc/environment",
  #       "echo 'DB_HOST=${var.DB_HOST}' | sudo tee -a /etc/environment",
  #       "echo 'DB_PORT=${var.DB_PORT}' | sudo tee -a /etc/environment",
  #       "echo 'FASTAPI_HOSTNAME=${var.FASTAPI_HOSTNAME}' | sudo tee -a /etc/environment",
  #       "echo 'FASTAPI_PORT=${var.FASTAPI_PORT}' | sudo tee -a /etc/environment",
  #       "echo 'TEST_HOSTNAME=${var.TEST_HOSTNAME}' | sudo tee -a /etc/environment",
  #       "echo 'AWS_S3_BUCKET=${var.AWS_S3_BUCKET}' | sudo tee -a /etc/environment",
  #       "echo 'AWS_REGION=${var.AWS_REGION}' | sudo tee -a /etc/environment"
  #     ]
  #   }
}

#123

