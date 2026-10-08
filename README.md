<p align="center">
    <img alt="" src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Keycloak-00b8e3?style=for-the-badge&logo=keycloak&logoColor=4D4D4D">
    <img alt="" src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white">
</p>

## Project Assumptions

Application with air quality data from IoT sensors.

## Technology Stack

### Back-end:

- Spring Boot.
- Java.
- Gradle Kotlin DSL.
- MongoDB.
- JUnit.
- Mockito.
- Lombok.
- Spring Security.
- Spring Data MongoDB.
- Spring Web.
- OpenAPI (Swagger).

### Authentication:

- **Identity provider**: Keycloak (realm `freshly`); the application never sees a password.
- **Pattern**: BFF with Spring Security's OAuth2 client; the browser holds only a session cookie.
- **Session store**: Redis (Spring Session).
- **JWT**: the back-end is a stateless resource server; a client can also call it with a Bearer token.
- **Details**: [auth module](./freshly-backend/modules/auth/README.md).

### Core back-end:

- Gradle build system.
- The application has an exception handling mechanism.
- The application has a logging mechanism.
- The application has separate environments for dev and prod.
- The application has a dedicated configuration file.
- The application has Keycloak integration for authentication and authorization.
- The application has permission management based on keycloak roles.
- The application written in Domain-Driven Design (DDD) style.
- Optimistic locking for concurrent updates with Etag and If-Match header.
- And many other features that can be found in the application code.

### Other:

- Docker for development environment.
- PMD for static code analysis.
- SpotBugs for static code analysis.
- JSpecify for null-safety annotations.
- NullAway for null-safety checks.
- Error Prone for static code analysis.
- Spotless for code formatting.
