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

- **Identity provider**: Keycloak (realm `freshly`) owns every page that touches a credential: sign-up, sign-in, email
  verification, password reset, two-factor authentication and acceptance of the terms of use. The application never sees
  a password.
- **Pattern**: BFF. The back-end signs users in with the authorization code flow and PKCE and keeps the tokens in its
  session; the browser holds only the `FRESHLY_SESSION` cookie (`HttpOnly`, `SameSite=Lax`, `Secure` in production). No
  token reaches JavaScript.
- **Session store**: Redis (Spring Session, `freshly:session` namespace).
- **JWT**: for a request with a session, the back-end attaches the access token itself and checks it like any other: the
  API is a stateless OAuth2 resource server verifying signature, issuer, expiry and audience (`freshly-api`). A client
  with its own token calls it with `Authorization: Bearer`.
- **State**: the API is stateless: every request is authorized by the JWT alone, so any instance can serve it. The only
  state is the browser session, and it lives in Redis, outside the application.
- **Token lifecycle**: access tokens live five minutes; every refresh returns a new refresh token and invalidates the
  old one, and concurrent requests of one session share a single refresh. Signing out revokes the refresh token at
  Keycloak.
- **Cross-site requests**: `SameSite=Lax` plus `Sec-Fetch-Site`, so a write or a logout sent from another site gets no
  token and is refused.
- **Accounts**: created in MongoDB at the first sign-in; permissions are granted to Keycloak roles.

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
