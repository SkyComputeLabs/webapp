packer {
  required_plugins {
    amazon = {
      source  = "github.com/hashicorp/amazon"
      version = ">= 0.0.1, < 2.0.0"
    }
    googlecompute = {
      source  = "github.com/hashicorp/googlecompute"
      version = ">= 1.0.0"
    }
  }
}

#variables
variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "gcp_project_id" {
  type    = string
  default = "csye6225-dev-451900"
}

variable "gcp_zone" {
  type    = string
  default = "us-central1-a"
}

variable "aws_source_ami" {
  default = "ami-04ffc9f7871904759"
}

#build images for AWS and GCP
source "amazon-ebs" "ubuntu" {
  ami_name      = "csye6225-${formatdate("YYYY-MM-DD_HH_mm_ss", timestamp())}"
  instance_type = "t2.small"
  region        = var.aws_region
  source_ami    = var.aws_source_ami
  ssh_username = "ubuntu"
  tags = {
    Name = "health-check-api"
  }
}

source "googlecompute" "ubuntu" {
  project_id          = var.gcp_project_id
  source_image_family = "ubuntu-2204-lts"
  zone                = var.gcp_zone
  image_name          = "csye6225-${formatdate("YYYY-MM-DD-HH-mm-ss", timestamp())}"
  ssh_username        = "ubuntu"
}

build {
  name = "user-creation-testing"
  sources = [
    "source.amazon-ebs.ubuntu",
    "source.googlecompute.ubuntu"
  ]

  #Installs application dependencies (Java 17).
  provisioner "shell" {
    inline = [
      "export DEBIAN_FRONTEND=noninteractive",
      "sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 437D05B5",
      "sudo apt-get update -o Acquire::Retries=1",
      "sudo apt-get install -y postgresql postgresql-client postgresql-contrib -o Acquire::Retries=1",
      "sudo apt-get install -y openjdk-17-jdk -o Acquire::Retries=1",
      "sudo apt-get install -y tomcat9 -o Acquire::Retries=1",
      "sudo apt-get install -y maven -o Acquire::Retries=1",
      "sudo apt-get install -y unzip -o Acquire::Retries=1",
      "sudo apt-get install -y awscli -o Acquire::Retries=1",
      "sudo apt-get install -y git -o Acquire::Retries=1",
      "sudo groupadd csye6225",
      "sudo useradd -g csye6225 -s /usr/sbin/nologin csye6225",
      "sudo mkdir -p /opt/csye6225",
      "sudo chown -R csye6225:csye6225 /opt/csye6225",
      "sudo chmod -R 750 /opt/csye6225",
    ]
  }

  # Copies the entire webapp directory to the instance
  provisioner "file" {
    source      = "." # Current directory 
    destination = "/opt/csye6225/webapp"
  }

  # Changes ownership and runs setup script
  provisioner "shell" {
    inline = [
      "sudo chown -R csye6225:csye6225 /opt/csye6225/webapp",
      "sudo bash /opt/csye6225/webapp/setup.sh"
    ]
  }

  # #Moves the application jar file to the desired location.
  # provisioner "shell" {
  #   inline = [
  #     "sudo mv /tmp/health-check-api-0.0.1-SNAPSHOT.jar /opt/csye6225/",
  #     "sudo chown csye6225:csye6225 /opt/csye6225/health-check-api-0.0.1-SNAPSHOT.jar",
  #   ]
  # }

  # #Copies the systemd service file to the instance.
  # provisioner "file" {
  #   source      = "csye6225.service"
  #   destination = "/tmp/csye6225.service"
  # }

  # #Adds and enables a systemd service file
  # provisioner "shell" {
  #   inline = [
  #     "sudo mv /tmp/csye6225.service /etc/systemd/system/",
  #     "sudo systemctl daemon-reload",
  #     "sudo systemctl enable csye6225.service",
  #     "sudo systemctl start csye6225.service",
  #   ]
  # }


}