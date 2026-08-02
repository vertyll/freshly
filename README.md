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
- Spring Mail.
- OpenAPI (Swagger).

### Authentication:

- Keycloak.

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

> [!NOTE]
>
> During application development, SOLID principles, DRY, composition over inheritance, dependency injection, design 
> patterns, architectural patterns were applied, tests were written, and other good programming practices were adopted.
