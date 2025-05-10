# ChatApp

A microservice-based chat application built with Spring Boot, featuring JWT authentication, REST APIs, and real-time messaging via WebSocket.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Features](#features)
4. [Tech Stack](#tech-stack)
5. [Prerequisites](#prerequisites)
6. [Setup](#setup)

   * [Database](#database)
   * [Configuration](#configuration)
   * [Building and Running](#building-and-running)
7. [API Endpoints](#api-endpoints)

   * [AuthService (Port 8082)](#authservice-port-8082)
   * [ChatService (Port 8080)](#chatservice-port-8080)
8. [WebSocket Usage](#websocket-usage)
9. [Project Structure](#project-structure)
10. [Future Enhancements](#future-enhancements)
11. [Contributing](#contributing)
12. [License](#license)

## Overview

ChatApp consists of two Spring Boot microservices with a common security module:

* **AuthService**: Handles user registration and authentication, issues JWT tokens.
* **ChatService**: Secured by JWT, provides REST and WebSocket endpoints for messaging.
* **common-security**: Shared JWT security components used by both services.

**Client** applications (web browsers, Postman, etc.) interact with these services over HTTP (REST) and WebSocket (STOMP over SockJS).

## Architecture

```text
                           +-------------------+
                           |                   |
                           | common-security   |
                           | (JWT Components)  |
                           |                   |
                           +---^-------^-------+
                                |       |
                 depends on     |       |     depends on
                                |       |
           +-------------------+         +-------------------+
           |                   |  JWT    |                   |
           |   AuthService     | <-----> |   ChatService     |
           |   (Register,      |         |   (Messaging)     |
           |    Login)         |         |                   |
           +--------+----------+         +--------+----------+
                    |                             ^
                    | REST API / WebSocket        |
                    v                             |
           +--------+----------+                  |
           |                   |                  |
           |      Client       | -----------------+
           |   (Browser,       |
           |    Postman)       |
           +-------------------+
```

## Features

* **Microservice Architecture**
* **Shared Security Module** to eliminate code duplication
* **JWT Authentication** (Bearer tokens)
* **REST API** for user management and message history
* **WebSocket (STOMP over SockJS)** for real-time messaging
* **Spring Security** for securing HTTP and WebSocket endpoints
* **Spring Data JPA (Hibernate)** with PostgreSQL
* **Thymeleaf UI** in ChatService for simple web interface
* **Lombok** to reduce boilerplate
* **Centralized Exception Handling**
* **DTO-level Validation**

## Tech Stack

| Component        | Technology                             |
| ---------------- | -------------------------------------- |
| Language         | Java 17+                               |
| Framework        | Spring Boot 3.x                        |
| Security         | Spring Security 6.x, jjwt              |
| Data Persistence | Spring Data JPA, Hibernate, PostgreSQL |
| Messaging        | Spring WebSocket (STOMP), SockJS       |
| Build Tool       | Maven 3.6+                             |
| UI               | Thymeleaf                              |
| Utilities        | Lombok                                 |

## Prerequisites

* JDK 17 or higher
* Maven 3.6 or higher
* PostgreSQL server
* HTTP client (Postman, Insomnia, curl, etc.)

## Setup

### Database

Create two PostgreSQL databases:

```sql
CREATE DATABASE auth_db;
CREATE DATABASE chat_db;
```

Ensure a PostgreSQL user has privileges for both databases.

### Configuration

Update `src/main/resources/application.properties` (or `application.yml`) in each service:

**AuthService (`authService`)**

```properties
server.port=8082

spring.datasource.url=jdbc:postgresql://localhost:5432/auth_db
spring.datasource.username=<DB_USER>
spring.datasource.password=<DB_PASSWORD>

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=true

# JWT settings
jwt.secret=<YOUR_SECRET_KEY>
jwt.expiration.ms=3600000
```

**ChatService (`chatService`)**

```properties
server.port=8080

spring.datasource.url=jdbc:postgresql://localhost:5432/chat_db
spring.datasource.username=<DB_USER>
spring.datasource.password=<DB_PASSWORD>

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=true

# JWT (must match AuthService)
jwt.secret=<YOUR_SECRET_KEY>
jwt.expiration.ms=3600000
```

### Building and Running

Build and run the modules in the following order:

```bash
# First build the common security module
mvn clean install -pl common-security

# Build and run AuthService
cd authService
mvn clean package
java -jar target/authService-0.0.1-SNAPSHOT.jar

# Build and run ChatService
cd chatService
mvn clean package
java -jar target/chatService-0.0.1-SNAPSHOT.jar
```

Alternatively, build all modules at once:

```bash
# Build all modules
mvn clean package

# Run each service
java -jar authService/target/authService-0.0.1-SNAPSHOT.jar
java -jar chatService/target/chatService-0.0.1-SNAPSHOT.jar
```

## API Endpoints

### AuthService (Port 8082)

* **Register User**

  ```http
  POST /api/v1/auth/register
  Content-Type: application/json

  {
    "username": "testuser",
    "password": "password123"
  }
  ```

* **Login**

  ```http
  POST /api/v1/auth/login
  Content-Type: application/json

  {
    "username": "testuser",
    "password": "password123"
  }
  ```

  Response:

  ```json
  {
    "accessToken": "<token>",
    "tokenType": "Bearer",
    "username": "testuser"
  }
  ```

* **Get Current User**

  ```http
  GET /api/v1/auth/me
  Authorization: Bearer <token>
  ```

### ChatService (Port 8080)

All endpoints require `Authorization: Bearer <token>`.

* **Send Message**

  ```http
  POST /api/v1/messages
  Content-Type: application/json
  Authorization: Bearer <token>

  {
    "chatRoomId": "general",
    "content": "Hello everyone!"
  }
  ```

* **Get Message History**

  ```http
  GET /api/v1/messages/{chatRoomId}
  Authorization: Bearer <token>
  ```

## WebSocket Usage

* **Connect**: `ws://localhost:8080/ws` (SockJS fallback)
* **STOMP Headers**: `Authorization: Bearer <token>`
* **Send**: Destination `/app/chat.sendMessage`

  ```json
  {
    "chatRoomId": "general",
    "content": "Hello via WebSocket!"
  }
  ```
* **Subscribe**: `/topic/messages/{chatRoomId}`
* **Error Queue**: `/user/queue/errors`

## Project Structure

```text
ChatService/
├── authService/
│   ├── src/main/java/iwkms/chatapp/authservice
│   │   ├── config
│   │   ├── controller
│   │   ├── dto
│   │   ├── exception
│   │   ├── model
│   │   ├── repository
│   │   ├── service
│   │   └── AuthServiceApplication.java
│   └── src/main/resources/application.properties
├── chatService/
│   ├── src/main/java/iwkms/chatapp/chatservice
│   │   ├── config
│   │   ├── controller
│   │   ├── dto
│   │   ├── exception
│   │   ├── model
│   │   ├── repository
│   │   ├── service
│   │   └── ChatServiceApplication.java
│   ├── src/main/resources/static
│   ├── src/main/resources/templates
│   └── src/main/resources/application.properties
├── common-security/
│   └── src/main/java/iwkms/chatapp/common/security
│       ├── config
│       ├── jwt
│       └── websocket
├── pom.xml
└── README.md
```

## Future Enhancements

* Service discovery (Eureka, Consul)
* API Gateway (Spring Cloud Gateway)
* Centralized configuration (Spring Cloud Config)
* Monitoring & logging (Prometheus, Grafana, ELK)
* Docker & Docker Compose
* Enhanced test coverage (unit & integration)
* Role-based access control
* User presence handling
* Private messaging
* Asynchronous messaging (RabbitMQ, Kafka)

## Contributing

Contributions are welcome! Please fork the repository, create a feature branch, and submit a pull request.

## License

This project is licensed under the [MIT License](LICENSE).
