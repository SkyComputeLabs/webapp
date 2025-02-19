# Web Application

This repository contains the code for our web application built with Spring Boot, PostgreSQL, and tested with Postman.

## Prerequisites

Before you begin, ensure you have met the following requirements:

* Java Development Kit (JDK) 11 or later
* Maven 3.6 or later
* PostgreSQL 12 or later
* Postman for API testing
* Git for version control

## Building and Deploying the Application Locally

To build and deploy this application locally, follow these steps:

1. Clone the repository:
https://github.com/Shrutkeerti200/webapp.git


2. Navigate to the project directory:
cd webapp


3. Create a PostgreSQL database for the application:


4. Update the `application.properties` file in `src/main/resources` with your database credentials:
spring.datasource.url=jdbc:postgresql://localhost:5432/webapp_db
spring.datasource.username=your_username
spring.datasource.password=your_password


5. Build the project:
mvn clean install


6. Run the application:
mvn spring-boot:run


7. The application should now be running on `http://localhost:8080`

## API Testing

Use Postman to test the API endpoints. Import the Postman collection provided in the `postman` directory of this repository.

## Development Workflow

1. Fork this repository to your GitHub account.
2. Clone your forked repository locally.
3. Create a new branch for each feature or bug fix.
4. Make your changes and commit them with descriptive commit messages.
5. Push your changes to your fork on GitHub.
6. Create a pull request to the main repository.

## Contributing

Contributions to this project are welcome. Please ensure you follow these guidelines:

1. Fork the repository.
2. Create a new branch for each feature or improvement.
3. Send a pull request from each feature branch to the develop branch.

Project Link: https://github.com/SkyComputeLabs/webapp
