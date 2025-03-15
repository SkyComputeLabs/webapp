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

# Variables
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
  default = "us-east1-b"
}

variable "aws_source_ami" {
  default = "ami-04ffc9f7871904759"
}

variable "db_user" {
  type    = string
  default = "root"
}

variable "db_pass" {
  type    = string
  default = "root"
}

# Build images for AWS and GCP
source "amazon-ebs" "ubuntu" {
  ami_name      = "csye6225-${formatdate("YYYY-MM-DD_HH_mm_ss", timestamp())}"
  instance_type = "t2.micro"
  region        = var.aws_region
  source_ami    = "ami-0f9de6e2d2f067fca"
  vpc_filter {
    filters = {
      is-default = "true"
    }
  }
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
  ssh_timeout  = "10m" # Increase SSH timeout
  tags = {
    Name = "health-check-api"
  }
}

source "googlecompute" "ubuntu" {
  project_id          = var.gcp_project_id
  source_image_family = "ubuntu-2204-lts"
  machine_type        = "e2-micro"
  zone                = var.gcp_zone
  image_name          = "csye6225-${formatdate("YYYY-MM-DD-HH-mm-ss", timestamp())}"
  ssh_username        = "ubuntu"
  ssh_timeout         = "10m"
}

build {
  name = "user-creation-testing"
  sources = [
    "source.amazon-ebs.ubuntu",
    "source.googlecompute.ubuntu"
  ]

  # Build the JAR first
  provisioner "shell-local" {
    command = "mvn clean package"
  }

  # Provisioner to install dependencies
  provisioner "shell" {
    environment_vars = [
      "DB_USER=${var.db_user}",
      "DB_PASS=${var.db_pass}"
    ]
    inline = [
      "set -euxo pipefail",
      "cloud-init status --wait",
      "export DEBIAN_FRONTEND=noninteractive",
      "sudo apt-get update -y && sudo apt-get upgrade -y",
      "sudo apt-get install -y software-properties-common", # Required for add-apt-repository
      # Add PostgreSQL repository and key
      "sudo apt-get install -y wget ca-certificates",                                                                                      # Install wget and certificates
      "wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo tee /etc/apt/trusted.gpg.d/postgresql.asc > /dev/null", # Use tee to output to file
      "sudo apt-get update -y",
      "sudo apt-get install -y openjdk-17-jdk maven postgresql postgresql-contrib unzip",

      #Ensure that the PostgreSQL service is started and enabled
      "sudo systemctl start postgresql",
      "sudo systemctl enable postgresql",

      #Create the database and user for the application
      "sudo -i -u postgres psql -c \"CREATE DATABASE healthcheck_db;\"",
      "sudo -i -u postgres psql -c \"CREATE USER $DB_USER WITH PASSWORD '$DB_PASS';\"",
      "sudo -i -u postgres psql -c \"GRANT ALL PRIVILEGES ON DATABASE healthcheck_db TO $DB_USER;\"",

      #Create a group and user for the application
      "sudo groupadd csye6225",
      "sudo useradd -g csye6225 -s /usr/sbin/nologin csye6225",

      # Create application directory
      "sudo mkdir -p /opt/csye6225",

      # Set ownership and permissions
      "sudo chown -R csye6225:csye6225 /opt/csye6225",
      "sudo chmod -R 750 /opt/csye6225",
    ]
  }

  # Upload the application jar file to the instance
  provisioner "file" {
    source      = "target/health-check-api-0.0.1-SNAPSHOT.jar",
    destination = "/tmp/application.jar"
  }

  # Upload the application.properties file to the instance
  provisioner "file" {
    source      = "csye6225.service"
    destination = "/tmp/csye6225.service"
  }

  # Shell provisioner to move files and configure systemd
  provisioner "shell" {
    inline = [
      # Move files
      "sudo mv /tmp/application.jar /opt/csye6225/",
      "sudo mv /tmp/csye6225.service /etc/systemd/system/",

      # Set ownership and permissions
      "sudo chown csye6225:csye6225 /opt/csye6225/application.jar",
      "sudo chmod 750 /opt/csye6225/application.jar",
      "sudo chown root:root /etc/systemd/system/csye6225.service",
      "sudo chmod 644 /etc/systemd/system/csye6225.service",


      # Systemd configuration
      "sudo systemctl daemon-reload",
      "sudo systemctl enable csye6225.service",
      "sudo systemctl start csye6225.service",
      "sudo systemctl status csye6225.service"
    ]
  }
}


# Build images for AWS and GCP
# source "amazon-ebs" "ubuntu" {
#   ami_name      = "csye6225-${formatdate("YYYY-MM-DD_HH_mm_ss", timestamp())}"
#   instance_type = "t2.micro"
#   region        = var.aws_region
#   source_ami    = var.aws_source_ami
#   ssh_username  = "ubuntu"
#   tags = {
#     Name        = "health-check-api"
#     Environment = "Dev"
#   }
#   ssh_timeout = "10m" # Increase SSH timeout
# }

# source "googlecompute" "ubuntu" {
#   project_id          = var.gcp_project_id
#   source_image_family = "ubuntu-2204-lts"
#   zone                = var.gcp_zone
#   image_name          = "csye6225-${formatdate("YYYY-MM-DD-HH-mm-ss", timestamp())}"
#   ssh_username        = "ubuntu"
#   ssh_timeout         = "10m" # Increase SSH timeout
# }

# build {
#   name = "user-creation-testing"
#   sources = [
#     "source.amazon-ebs.ubuntu",
#     "source.googlecompute.ubuntu"
#   ]

#   # Upload webapp.zip to the instance
#   provisioner "file" {
#     source      = "./webapp.zip"
#     destination = "/tmp/webapp.zip"
#   }

#   # Install dependencies and configure the system
#   provisioner "shell" {
#     inline = [
#       "export DEBIAN_FRONTEND=noninteractive",

#       # Add PostgreSQL repository
#       "echo 'deb http://apt.postgresql.org/pub/repos/apt/ jammy-pgdg main' | sudo tee /etc/apt/sources.list.d/pgdg.list",
#       "wget --quiet https://www.postgresql.org/media/keys/ACCC4CF8.asc -O - | sudo apt-key add -",

#       # Update package list
#       "sudo apt-get update -o Acquire::Retries=1",

#       # Install common dependencies (excluding awscli and openjdk-17-jdk)
#       "sudo apt-get install -y postgresql postgresql-client postgresql-contrib apt-transport-https ca-certificates curl gnupg lsb-release unzip git",

#       # Install OpenJDK 11
#       "sudo apt-get install -y openjdk-11-jdk",

#       # Install AWS CLI
#       "curl \"https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip\" -o \"awscliv2.zip\"",
#       "unzip awscliv2.zip",
#       "sudo ./aws/install",

#       # Start and enable PostgreSQL service
#       "sudo systemctl start postgresql",
#       "sudo systemctl enable postgresql",

#       # Create database only if it doesn't exist
#       "sudo -u postgres psql -c \"DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'healthcheck_db') THEN CREATE DATABASE healthcheck_db; END IF; END $$;\"",

#       # Create user and grant privileges
#       "sudo -u postgres psql -c \"CREATE USER \\\"${var.db_user}\\\" WITH PASSWORD \\\"${var.db_pass}\\\";\"",
#       "sudo -u postgres psql -c \"GRANT ALL PRIVILEGES ON DATABASE healthcheck_db TO \\\"${var.db_user}\\\";\"",

#       # Create application directory
#       "sudo mkdir -p /opt/csye6225",
#       "sudo chown -R csye6225:csye6225 /opt/csye6225",
#       "sudo chmod -R 750 /opt/csye6225",

#       # Unzip webapp.zip
#       "unzip /tmp/webapp.zip -d /tmp/webapp",

#       # Move .jar file to its final location
#       "sudo mv /tmp/webapp/target/*.jar /home/csye6225/webapp.jar",
#       "sudo chown csye6225:csye6225 /home/csye6225/webapp.jar",

#       # Move and configure application.properties
#       "sudo mv /tmp/webapp/src/main/resources/application.properties /home/csye6225/",
#       "sudo chown csye6225:csye6225 /home/csye6225/application.properties",
#       "sudo sed -i \"s/DB_USER/${var.db_user}/g\" /home/csye6225/application.properties",
#       "sudo sed -i \"s/DB_PASS/${var.db_pass}/g\" /home/csye6225/application.properties",

#       # Move systemd service file
#       "sudo mv /tmp/webapp/csye6225.service /etc/systemd/system/csye6225.service",

#       # Reload systemd and start service
#       "sudo systemctl daemon-reload",
#       "sudo systemctl enable csye6225.service",
#       "sudo systemctl start csye6225.service",

#       # Verify installations
#       "psql --version",
#       "java --version",
#       "aws --version", # Verify AWS CLI installation
#       "systemctl status postgresql",
#       "systemctl status csye6225.service"
#     ]
#     environment_vars = [
#       "DB_USER=${var.db_user}",
#       "DB_PASS=${var.db_pass}"
#     ]
#   }


#   # Manifest Post-Processor
#   post-processor "manifest" {
#     output     = "./manifest.json"
#     strip_path = true

#   }

# }

















# packer {
#   required_plugins {
#     amazon = {
#       source  = "github.com/hashicorp/amazon"
#       version = ">= 0.0.1, < 2.0.0"
#     }
#     googlecompute = {
#       source  = "github.com/hashicorp/googlecompute"
#       version = ">= 1.0.0"
#     }
#   }
# }

# #variables
# variable "aws_region" {
#   type    = string
#   default = "us-east-1"
# }

# variable "gcp_project_id" {
#   type    = string
#   default = "csye6225-dev-451900"
# }

# variable "gcp_zone" {
#   type    = string
#   default = "us-east1-b"
# }

# variable "aws_source_ami" {
#   default = "ami-04ffc9f7871904759"
# }

# variable "db_user" {
#   type = string
#   default = "root"
# }

# variable "db_pass" {
#   type = string
#   default = "root"
# }

# #  variable "jar_file" {
# #   type    = string
# #   default = "./tmp/health-check-api-0.0.1-SNAPSHOT.jar"
# # }

# #build images for AWS and GCP
# source "amazon-ebs" "ubuntu" {
#   ami_name      = "csye6225-${formatdate("YYYY-MM-DD_HH_mm_ss", timestamp())}"
#   instance_type = "t2.micro"
#   region        = var.aws_region
#   source_ami    = var.aws_source_ami
#   ssh_username  = "ubuntu"
#   tags = {
#     Name = "health-check-api"
#   }
# }

# source "googlecompute" "ubuntu" {
#   project_id          = var.gcp_project_id
#   source_image_family = "ubuntu-2204-lts"
#   zone                = var.gcp_zone
#   image_name          = "csye6225-${formatdate("YYYY-MM-DD-HH-mm-ss", timestamp())}"
#   ssh_username        = "ubuntu"
# }

# build {
#   name = "user-creation-testing"
#   sources = [
#     "source.amazon-ebs.ubuntu",
#     "source.googlecompute.ubuntu"
#   ]

#   # Upload webapp.zip to the instance
#   provisioner "file" {
#     source      = "./webapp.zip"
#     destination = "/tmp/webapp.zip"
#   }

#   # Unzip webapp.zip and move files to their final locations
#   provisioner "shell" {
#     inline = [
#       # Unzip webapp.zip
#       "unzip /tmp/webapp.zip -d /tmp/webapp",

#       # Move .jar file to its final location
#       "sudo mv /tmp/webapp/target/*.jar /home/csye6225/webapp.jar",
#       "sudo chown csye6225:csye6225 /home/csye6225/webapp.jar",

#       # Move application.properties to its final location
#       "sudo mv /tmp/webapp/src/main/resources/application.properties /home/csye6225/",
#       "sudo chown csye6225:csye6225 /home/csye6225/application.properties",

#       # Move csye6225.service file to its final location
#       "sudo mv /tmp/webapp/csye6225.service /etc/systemd/system/csye6225.service",

#       # Reload systemd daemon and enable/start the service
#       "sudo systemctl daemon-reload",
#       "sudo systemctl enable csye6225.service",
#       "sudo systemctl start csye6225.service"
#     ]
#   }
# }

#   #Installs application dependencies.
#   provisioner "shell" {
#     inline = [
#       "export DEBIAN_FRONTEND=noninteractive",
#       "sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 437D05B5",
#       "sudo apt-get update -o Acquire::Retries=1",
#       "sudo apt-get install -y postgresql postgresql-client postgresql-contrib -o Acquire::Retries=1",
#       "sudo apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release",
#       "sudo apt-get install -y openjdk-17-jdk -o Acquire::Retries=1",
#       "sudo apt-get install -y tomcat9 -o Acquire::Retries=1",
#       "sudo apt-get install -y maven -o Acquire::Retries=1",
#       "sudo apt-get install -y unzip -o Acquire::Retries=1",
#       "sudo apt-get install -y awscli -o Acquire::Retries=1",
#       "sudo apt-get install -y git -o Acquire::Retries=1",
#       # Start and enable the PostgreSQL service
#       "sudo systemctl start postgresql",
#       "sudo systemctl enable postgresql",

#       # Create a database and user for the application
#       "sudo -u postgres psql -c \"CREATE DATABASE IF NOT EXISTS healthcheck_db;\"",
#       "sudo -u postgres psql -c \"CREATE USER \\\"${var.db_user}\\\" WITH PASSWORD \\\"${var.db_pass}\\\";\"'",
#       "sudo -u postgres psql -c \"GRANT ALL PRIVILEGES ON DATABASE healthcheck_db TO \\\"${var.db_user}\\\";\"'",
#       # Create a group and user for the application
#       "sudo groupadd csye6225",
#       "sudo useradd -g csye6225 -s /usr/sbin/nologin csye6225",
#       # Create the directory for the application and set permissions
#       "sudo mkdir -p /opt/csye6225",
#       "sudo chown -R csye6225:csye6225 /opt/csye6225",
#       "sudo chmod -R 750 /opt/csye6225",

#       # Replace placeholders in application.properties using environment variables
#       "/bin/bash -c 'sed -i \"s/DB_USER/${var.db_user}/g\" /tmp/application.properties'",
#       "/bin/bash -c 'sed -i \"s/DB_PASS/${var.db_pass}/g\" /tmp/application.properties'"
#     ]
#     environment_vars = [
#        "DB_USER=${var.db_user}",
#        "DB_PASS=${var.db_pass}"
#    ]
#   }

# provisioner "file" {
#   source      = var.jar_file
#   destination = "/tmp/health-check-api-0.0.1-SNAPSHOT.jar"
# }


#   # Copies the entire webapp directory to the instance
#   # provisioner "file" {
#   #   # source      = "../target/health-check-api-0.0.1-SNAPSHOT.jar"
#   #   source      = "health-check-api-0.0.1-SNAPSHOT.jar"
#   #   destination = "/tmp/health-check-api-0.0.1-SNAPSHOT.jar"
#   #   # generated   = true
#   # }

#   provisioner "shell"{
#     inline = [
#       "sudo mv /tmp/health-check-api-0.0.1-SNAPSHOT.jar /home/csye6225/",
#       "sudo chown csye6225:csye6225 /home/csye6225/health-check-api-0.0.1-SNAPSHOT.jar",
#     ]
#   }

#   provisioner "file" {
#     source      = "./src/main/resources/application.properties"
#     destination = "/tmp/application.properties"
#     # generated   = true
#   }

#   provisioner "shell"{
#     inline = [
#       "sudo mv /tmp/application.properties /home/csye6225/",
#       "sudo chown csye6225:csye6225 /home/csye6225/application.properties",
#     ]
#      environment_vars = [
#        "DB_USER=${var.db_user}",
#        "DB_PASS=${var.db_pass}"
#     ]
#   }

#   # Copy the systemd service file to the instance
#   provisioner "file" {
#     source      = "./csye6225.service"
#     destination = "/tmp/csye6225.service"
#   }

#   # Unzip the application and configure it on the instance
#   provisioner "shell" {
#     inline = [
#       # "set -x",
#       # Set ownership of files to csye6225 user and group
#       # "sudo chown -R csye6225:csye6225 /opt/csye6225/ || exit 1",
#       # "sudo chmod +x /opt/csye6225/target/health-check-api-0.0.1-SNAPSHOT.jar || exit 1",
#       # # Move systemd service file to /etc/systemd/system/
#       "sudo mv /tmp/csye6225.service /etc/systemd/system/csye6225.service",
#       # Reload systemd daemon and enable/start the service
#       "sudo systemctl daemon-reload || exit 1",
#       "sudo systemctl enable csye6225.service || exit 1",
#       "sudo systemctl start csye6225.service || exit 1",
#       "sudo systemctl status csye6225.service || exit 1",
#     ]
#   }

#   # Add the manifest post-processor here
#   post-processor "manifest" {
#     output     = "./manifest.json"
#     strip_path = true
#   }
# }


# # #Moves the application jar file to the desired location.
# # provisioner "shell" {
# #   inline = [
# #     "sudo mv /tmp/health-check-api-0.0.1-SNAPSHOT.jar /opt/csye6225/",
# #     "sudo chown csye6225:csye6225 /opt/csye6225/health-check-api-0.0.1-SNAPSHOT.jar",
# #   ]
# # }

# # #Copies the systemd service file to the instance.
# # provisioner "file" {
# #   source      = "csye6225.service"
# #   destination = "/tmp/csye6225.service"
# # }

# # #Adds and enables a systemd service file
# # provisioner "shell" {
# #   inline = [
# #     "sudo mv /tmp/csye6225.service /etc/systemd/system/",
# #     "sudo systemctl daemon-reload",
# #     "sudo systemctl enable csye6225.service",
# #     "sudo systemctl start csye6225.service",
# #   ]
# # }


