# Hexagonal Layering

How the dependency rule is enforced in a single-process application, and what it cost.

## The rule, inside one bounded context

```text
infrastructure ──► application ──► domain
```

| Layer            | May depend on                | Contains                                                                     |
|------------------|------------------------------|------------------------------------------------------------------------------|
| `domain`         | JDK, JSpecify, `shared-lang` | Aggregates, value objects, repository *ports*, policies, the error catalogue |
| `application`    | `domain` plus `shared-authz` | Use cases, commands, response DTOs, inbound/outbound ports                   |
| `infrastructure` | everything                   | MongoDB, web, HTTP clients, Spring wiring, adapters, ACL                     |

## The rule, between bounded contexts

A distributed system gets this for free: when a module boundary is a process boundary, the
compiler enforces it, because the other module's classes are not on disk.

In one process nothing stops it, and Freshly did it: `auth.application.AuthService`
imported `useraccess.application.UserAccessService` and
`notification.application.NotificationService` directly.

**Allowed:** `X-infrastructure` → `Y-application` (Y's inbound port).
**Forbidden:** anything → `Y-domain`, anything → `Y-infrastructure`, and
`X-application` → `Y-application`.

A module that needs something from another declares its **own outbound port** and
writes an adapter in its own infrastructure. That adapter is the anti-corruption
layer — the counterpart of what an Avro decoder does at a Kafka boundary.

```text
auth-application                     auth-infrastructure              useraccess-application
─────────────────                    ───────────────────              ──────────────────────
UserProvisioningPort  ◄──implements── UserAccessProvisioningAdapter ──calls──► UserAccessCommandUseCase
   (auth's own words)                    (translates)                            (useraccess's words)
```

Without it, a rename in `useraccess` breaks `auth`'s compilation, and `auth`'s use
cases end up holding `useraccess`'s DTOs as if they were their own vocabulary.

## Enforcement

Two independent mechanisms, overlapping on purpose.

### `checkHexagonalDependencies`

A Gradle task registered by the `freshly.domain-layer` and
`freshly.application-layer` convention plugins. It reads the **resolved**
`compileClasspath` and fails the build if any of these appear:

```
org.springframework   jakarta.persistence   jakarta.validation   jakarta.servlet
org.hibernate         com.fasterxml.jackson tools.jackson        org.slf4j
org.apache.logging    org.mongodb           org.mapstruct        org.projectlombok
io.jsonwebtoken       org.keycloak          org.apache.httpcomponents
```

Resolved rather than declared, because the easy failure is transitive: naming one
enum from a Spring-bound module puts the entire framework on the classpath, and a
declared-dependency check sees nothing wrong.

Run it alone with `./gradlew checkHexagonalDependencies`.

### PMD: `NoWildcardImports`

A wildcard makes the layering rules unreadable. `import org.springframework.*` in an
application layer is exactly what `checkHexagonalDependencies` exists to prevent, and a
reviewer scanning imports to see which frameworks a class touches learns nothing from a
star. With seven contexts holding deliberately similar names — `Email`,
`PermissionCatalogue`, `ApplicationBeansConfig` — it also decides silently which package a
name resolves to.

Enforced as an XPath rule in `config/pmd`, not as a bespoke Gradle task and not in Spotless.
Spotless cannot do it: expanding a star means knowing which types the file uses, which needs
type resolution and a classpath a text formatter does not have. `.editorconfig` raises
IntelliJ's collapse threshold so the rule is rarely hit in the first place.

### ArchUnit

`platform/shared-archunit`, applied per module with one class:

```java
class UserAccessArchitectureTest extends FreshlyArchitectureTest {
    UserAccessArchitectureTest() { super("com.vertyll.freshly.useraccess"); }
}
```

| Rule                                               | Guards                                                    |
|----------------------------------------------------|-----------------------------------------------------------|
| The dependency rule points inwards                 | `infrastructure` → `application` → `domain`               |
| Domain and application are framework-free          | at class level, where a classpath cannot see              |
| The inside does not depend on `web`                | the exact violation `AuthService` committed               |
| `@Document` only in `infrastructure.persistence`   | persistence stays an adapter detail                       |
| `@RestController` only in `infrastructure.web`     | HTTP is one delivery mechanism                            |
| Ports are interfaces                               | a port is a contract, not a class the inside instantiates |
| Adapters live in infrastructure                    | an adapter is by definition the outside edge              |
| The domain does not use Lombok's shape annotations | an aggregate is not a struct                              |
| Modules do not reach into each other's internals   | the boundary above                                        |
| Cross-module calls go through infrastructure       | forces the ACL                                            |

The task reads a classpath; the rules read classes. The task fails earlier and
says why more clearly; ArchUnit reaches what a classpath structurally cannot
express.

## What the framework-free rule costs

### `@Service` → explicit beans

There is no component scanning of the application layer.
`infrastructure/config/ApplicationBeansConfig` constructs each use case by hand.
Verbose, and in exchange every use case is constructible in a unit test with two
fakes and no Spring context — see `UserAccessCommandServiceTest`, which uses
`new`.

### `@Transactional` → a decorator at the port

`TransactionalUseCaseFactory` wraps each inbound port in a dynamic proxy running
every call inside a `TransactionTemplate`.

A proxy rather than one handwritten decorator per port: those are hundreds of
lines of pure delegation, and each new use-case method needs a matching edit or
silently runs outside a transaction. The proxy has no method to forget.

Read-only mode follows **which port it is**, not a list of method names. A query
port is read-only in its entirety, so nothing hand-maintained can drift out of
step with a rename. The version of this class that takes a list of names looks
more flexible and is strictly worse.

Only the wrapped proxy is registered as a bean, so nothing can obtain the bare
service and call it outside a transaction.

### SLF4J → a port

`UseCaseLogger` in `shared-lang`, `Slf4jUseCaseLogger` in `shared-infra`. Arguable — SLF4J is a facade, not a framework
— but admitting one exception makes the rule a sentence with a footnote, and a build check cannot
enforce a footnote. It also turns "did this use case log the refusal" into an
assertion rather than console output.

### Bean validation → the web adapter

Request DTOs live in `infrastructure/web/dto` with their `jakarta.validation`
annotations, because those constraints describe an **HTTP** contract enforced by
Spring MVC. Use cases take **commands** built by the controller — already parsed
and typed, so a use case can never receive a half-validated request.

Validation is split, not duplicated:

| Kind                                        | Enforced by         |
|---------------------------------------------|---------------------|
| Syntactic — required, format, length        | web adapter         |
| Invariants — a user holds at least one role | domain constructors |

The second must stay in the domain even where the DTO looks to cover it: the
aggregate is also reached from `auth`'s provisioning adapter, which never passes
through Spring MVC.

### Spring Data paging → three types of our own

| Type            | Layer              | Holds                                         |
|-----------------|--------------------|-----------------------------------------------|
| `PageRequest`   | crosses everything | the request: page index and size              |
| `PageResult`    | repository ports   | domain models, in whatever shape paging needs |
| `PagedResponse` | query use cases    | response DTOs, in the shape the API promises  |

Conversion to and from Spring's `Pageable` / `Page` happens in the persistence adapter,
and nowhere else. `PagedResponse.from` is the crossing point between the other two.

Two page types looks redundant until the day a query needs a field the repository does not
have, or the repository grows one no client should see. With one type, either is an API
change.

### Optimistic locking → `VersionGuard`

The comparison lives in `shared-lang` and the caller supplies the exception to
throw. A shared helper that throws Spring's `OptimisticLockingFailureException`
itself puts a framework leak inside the hexagon, wearing a helper's clothes.

`ETagUtil` sits on the other side, in `shared-web`: the comparison is a rule, the
header is transport.

### Lombok → infrastructure only

Refused in `domain` by an ArchUnit rule, freely used in adapters and documents.
Not inconsistency: a document *is* a data holder, so generating its shape is
right. An aggregate is not, and `@Data` next to an invariant makes the invariant
decorative.

## Error catalogues

Each context owns one, in `domain/error/`:

| Module       | Catalogue         |
|--------------|-------------------|
| `useraccess` | `UserAccessError` |
| `auth`       | `AuthError`       |
| `permission` | `PermissionError` |
| `airquality` | `AirQualityError` |

Each entry names a translation key and an `ErrorKind`. One handler —
`shared-web`'s `DomainExceptionHandler` — serves every module and knows none of
their catalogues, which is what let the three per-module `*ControllerAdvice`
classes collapse into one file.

Every refusal answers as an **RFC 9457 problem document** (`application/problem+json`):

```json
{
  "type":     "urn:freshly:error:error.useraccess.userNotFound",
  "title":    "Not Found",
  "status":   404,
  "detail":   "No such user.",
  "instance": "/api/v1/users/7d1c9a52-3a43-4c34-9a8f-2f5d7c6f1b10",
  "code":     "error.useraccess.userNotFound"
}
```

`type` is what a client branches on; `code` repeats the bare key so it need not parse the
URI; `detail` is the translated prose. Validation failures add a `fields` member, and a
`DomainException` carrying interpolation arguments adds `params`.

A page is `{ items, pagination: { total, page, pageSize, totalPages, hasMore } }` — the
resources under `items` with nothing mixed in, and the query's own facts nested beside
them.

Successes are otherwise the resource itself — no envelope. An envelope around a success
duplicates the status line and the `Date` header, and leaves the API answering in two shapes
depending on whether the request worked.

The status mapping happens once, in `ErrorHttpStatusMapper`. Spread across advices it would
be decided separately in each, so two refusals of the same kind could come back as different
statuses with nobody wrong and nowhere to agree.

## The platform

Five modules that are not bounded contexts, split by whether they carry a framework:

| Framework-free — anything may depend | Spring-bound — adapters only                    |
|--------------------------------------|-------------------------------------------------|
| `shared-lang`, `shared-authz`        | `shared-infra`, `shared-web`, `shared-security` |

The platform never depends on a bounded context. Where it needs an answer only a context
can give, it declares an SPI and the context implements it — `PermissionEvaluator` for
authorization decisions, `CacheSpec` for cache registration, `PermissionCatalogue` for the
permission list.

`PlatformArchitectureTest` in `bootstrap` enforces this, and it runs there because that is
the only project whose classpath carries both the whole platform and every module — which
is what the rule needs in order to be able to fail.

See `docs/shared-modules.md` for what may go in each and why.

## When a direct call, and when an event

The two channels a module may use to reach another are not interchangeable, and picking
between them has one test:

> **Who owns the reaction?**

If the calling module owns it, call the other module's inbound port through an
anti-corruption adapter. If a *different* module owns it, publish an event and let that
module subscribe.

The cross-module call in this application is the first kind. "When someone signs in for the
first time, give them an account" is `auth`'s policy: `useraccess` keeps accounts and has no
business knowing what a sign-in is.

So it is a direct call through an outbound port, not a domain event. An event would be
ceremony: `auth` publishing it and a listener in `auth-infrastructure` consuming it is a
round trip through the event bus inside one module, buying no decoupling and costing a
reader the ability to see what a sign-in triggers.

There is no `DomainEventPublisher` port either. An abstraction with no implementer and no
caller is speculative generality; when a reaction genuinely lands in another module, the port
is four lines and the event type belongs in the publisher's *application* layer, which is
already the one package another module may depend on.

## Composition

`bootstrap` is the only project with `bootJar`. Every `*-infrastructure` is a
plain library.

It imports one `*ModuleConfig` per context rather than scanning
`com.vertyll.freshly`. A broad scan means the module boundary exists in the source
tree and nowhere in the running application: any `@Component` anywhere becomes a
bean, published or not.
