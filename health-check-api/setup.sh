#!/bin/bash

#Update package lists
echo "Updating package lists..."
sudo apt update -y

#Upgrade installed pacakages
echo "Upgrading installed packages..."
sudo apt upgrade -y

#Install PostgreSQL if not installed
if ! command -v psql &> /dev/null; then
echo "Installing PostgreSQL..."
sudo apt install -y postgresql postgresql-contrib
else
echo "PostgreSQL is already installed"
fi

#Start PostgreSQL service
echo "Starting PostgreSQL service.."
sudo systemctl enable postgresql
sudo systemctl start postgresql

#create database and user
echo "Checking if database and user exist..."
sudo -i -u postgres psql <<EOF
CREATE DATABASE healthcheck_db;
CREATE USER healthcheck_user WITH ENCRYPTED PASSWORD '$(openssl rand -base64 12)';
GRANT ALL PRIVILEGES ON DATABASE healthcheck_db TO healthcheck_user;
EOF

#Create a new group for the application
if ! getent group csye6225_group > /dev/null; then
    sudo groupadd csye6225_group
    echo "Group created."
else
    echo "Group already exists."
fi

#Create a new user for the application
if ! id "csye6225_user" &>/dev/null; then
    sudo useradd -m -g csye6225_group csye6225_user
    echo "User created."
else
    echo "User already exists."
fi

#Unzip application in /opt/csye6225
echo "Extracting application files..."
sudo mkdir -p /opt/csye6225
sudo cp -R Shrutkeerti_Sangolkar_002304742_02/* /opt/csye6225/

#Update the permissions
echo "Updating the permissions..."
sudo chown -R csye6225_user:csye6225_group /opt/csye6225
sudo chmod -R 750 /opt/csye6225

echo "Checking if Java is installed..."
if ! command -v java &> /dev/null; then
    echo "Java not found. Installing OpenJDK..."
    sudo apt install -y openjdk-17-jdk
else
    echo "Java is already installed."
fi

echo "Setting JAVA_HOME..."
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
echo "export JAVA_HOME=$JAVA_HOME" >> ~/.bashrc
source ~/.bashrc

# Run the Spring Boot application
echo "Starting the Spring Boot application..."
cd /opt/csye6225/Shrutkeerti_Sangolkar_002304742_02/webapp/health-check-api
sudo -u csye6225_user nohup java -jar target/health-check-api-0.0.1-SNAPSHOT.jar > /dev/null 2>&1 &

# Wait for the application to start
echo "Waiting for the application to start..."
sleep 20

# Run test cases
echo "Running test cases..."
cd /opt/csye6225//Shrutkeerti_Sangolkar_002304742_02/webapp/health-check-api
sudo -u csye6225_user ./mvnw test

# Check if the application is running
if pgrep -f "health-check-api-0.0.1-SNAPSHOT.jar" > /dev/null
then
    echo "Spring Boot application is running."
else
    echo "Failed to start Spring Boot application."
fi

#Print completion message
echo "Setup completed successfully"
