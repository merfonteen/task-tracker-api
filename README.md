# Task Tracker

A simple task tracking application. This application allows to create task boards, place task states on them, and the tasks themselves inside them. Also you can invite someone in your task board and assign him task.

## Description

Task tracker is designed for small teams or individuals who need a lighweight task management tool. The system includes user authentication and task management.

## Technologies Used

- **Java 21**
- **Spring Boot 3**
  - Core
  - Web
  - Data
  - Security
- **PostgreSQL**
- **Hibernate**
- **Gradle**
- **JUnit 5**
- **Mockito**
- **Swagger** (for API documentation)
- **Docker**

## Installation and Setup

1. **Clone the repository:**
   ```bash
     git clone https://github.com/IXSirius/task-tracker-api.git
     cd task-tracker-api
   ````
   
2. **Make sure you have the following installed:**   
   **Java 21:** [Download Java](https://adoptium.net/temurin/releases/)   
   **Docker and Docker Compose**: [Install Docker](https://www.docker.com/get-started)

3. **Running the application without Docker:**    
   Update the `application.yml` file with your PostgreSQL credentials.
   ```bash
    ./gradlew bootRun
   ````

4. **Build the project:**
   ```bash
    ./gradlew build
   ````

5. **Run the application using Docker Compose:**
   ````bash
     docker-compose up
   ````

   **Alternatively**, you can run the application directly from Docker Hub:
   ```bash
     docker run -d -p 8080:8080 ixsirius/task-tracker-app:latest

6. **Access the application at:**  
   **Web Application:** [http://localhost:8080](http://localhost:8080)  
   **API Documentation:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui.html)

## Examples of requrest to API

### Note:
For most endpoints, you need to authenticate with a JWT token.  
1. **Register** a user and **log in** to get the token.
2. Use the token in the `Authorization` header for subsequent requests:
   
### Register a new user:
**POST** `/auth/register`
```json
{
  "email": "your email",
  "username": "your username",
  "password": "your password"
}
```

### Authorize a user:
**POST** `/auth/login`
```json
{
  "username": "your username",
  "password": "your password"
}
````

**Response example:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Create a new task board:
**POST** `/api/projects?name=TestName`  
**Headers:** `Authorization`: Bearer <your_token>

**Response example:**
```json
{
  "id": 1,
  "name": "TestName",
  "createdAt": "2024-11-28T12:00:00Z"
}
```



