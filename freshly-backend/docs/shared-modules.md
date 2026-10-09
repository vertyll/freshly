# Shared modules

What lives in `platform/`, why each one is allowed to exist, and what may go in it.

A shared module without written admission criteria becomes the place things go when nobody
wants to decide where they belong — and then everything depends on it, so everything depends
on everything it holds. The criteria below are what keep `platform/` from becoming that.

---

## The rule

> A type belongs in the platform only if **two or more contexts genuinely need the same
> one**, and it carries **no domain meaning**.

"Could plausibly be shared" is not the bar. `auth`'s `SignedInUser` and `useraccess`'s
`SystemUser` both describe a person and both carry roles, and they stay apart because they are
not the same concept: one is who just signed in, as Keycloak's token says, the other a standing
inside the application. Merging them would create exactly the coupling this rule exists to prevent,
and the merged type would immediately need to satisfy both.

---

## The modules

| Module            | Framework-free | Who may depend on it                      | Contains                                                                                                                                                                                      |
|-------------------|----------------|-------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `shared-lang`     | yes            | anything                                  | `DomainError`, `ErrorKind`, `DomainException`, `PageRequest`, `PageResult`, `PagedResponse`, `PaginationMeta`, `VersionGuard`, `UseCaseLogger`, `TranslationCatalogue`, `TranslationResolver` |
| `shared-authz`    | yes            | anything                                  | `PermissionDescriptor`, `PermissionCatalogue`, `StockRole`, `PermissionScope`, `CallerRoles`, `PermissionEvaluator`                                                                           |
| `shared-i18n`     | yes            | anything                                  | `IcuMessages`                                                                                                                                                                                 |
| `shared-infra`    | no             | `*-infrastructure`, `bootstrap`           | `TransactionalUseCaseFactory`, `Slf4jUseCaseLogger`, `CacheConfig`, `CacheSpec`                                                                                                               |
| `shared-web`      | no             | `*-infrastructure`, `bootstrap`           | `GlobalExceptionHandler`, `Problems`, `ErrorHttpStatusMapper`, `ETagUtil`, `MessageResolver`, the security annotations                                                                        |
| `shared-security` | no             | `bootstrap`                               | filter chain, method security, `AuthenticationRoles`, `CallerRolesArgumentResolver`, `KeycloakRealmRoleConverter`, `PublicEndpointRegistry`, `ProblemAuthenticationEntryPoint`                |
| `shared-archunit` | test-only      | `*-infrastructure` test, `bootstrap` test | the rules                                                                                                                                                                                     |

### `shared-lang` is not a Shared Kernel

In DDD a Shared Kernel is a **domain model** two contexts agree to share, and Evans
describes it as the integration pattern demanding the tightest coordination between
teams — something to reach for reluctantly.

This module is not that. `DomainError` and `PageRequest` are technical vocabulary, the
equivalent of a standard library, and depending on them commits a module to nothing about
how it models its own subject.

The name `shared-kernel` would be a liability twice over: it promises a stronger coupling
than exists, and a module called Shared Kernel is one people feel entitled to put domain
concepts into.

`sharedLanguageHoldsNoDomainConcept` in `PlatformArchitectureRules` is the mechanical
half of the guard; the criterion above is the real one.

### `shared-authz` is a Published Language

This one genuinely is shared vocabulary, and that is why it is separate from
`shared-lang`. Authorization is a conversation between the `permission` context and every
context that enforces a decision, and `PermissionDescriptor`, `CallerRoles` and
`PermissionEvaluator` are the words they use.

Framework-free, so it can appear in an application layer's signature.

### `shared-i18n` holds one class, and that is the point

It carries `IcuMessages` — compile-check a message pattern, report its placeholders, render
it — and the ICU4J dependency that makes those possible.

Two projects use it: `shared-web`, which renders every problem detail, and `translation-infrastructure`, which supplies
the `MessageGrammar` the aggregate validates against. Neither is a bounded context's inner layer, so the obvious
alternative is to put the class in `shared-web` and have the translation context depend on the web platform
for it. That reads wrong the moment it is written down: nothing about compiling a message
pattern is about HTTP.

The other alternative is `shared-lang`. That module is on to compile classpath of every
domain and application project in the application, and its whole claim is that depending on
it commits a module to nothing. Putting a formatting library there would widen that surface
for the five contexts that never format anything.

It is framework-free and `checkHexagonalDependencies` guards it like the other two. ICU4J is
a formatting library in the same category as `java.time`: no container, no lifecycle, nothing
to configure.

### `shared-infra` holds what every adapter needs identically

`Slf4jUseCaseLogger` and `TransactionalUseCaseFactory` are here rather than once per context.

"Infrastructure is a module's own" is a good rule that does not reach this far: owning your
infrastructure means owning the *choices* — which database, which mail transport — not
owning a private copy of a reflection proxy that would be byte-for-byte identical in every
context. Sharing costs nothing on the compile path either, since every `*-infrastructure`
already depends on `shared-web`.

The decisive argument is maintenance. `TransactionalUseCaseFactory` unwraps
`InvocationTargetException` so a `DomainException` does not reach the exception handler
disguised as a reflection failure. That is subtle enough that a copy per context means a fix
applied N times with nothing to notice a miss.

---

## Two SPIs, and why they point the way they do

The platform must never depend on a bounded context. Twice it needs something only a
context can answer, and both times the answer is an interface the platform owns and a
context implements.

### `PermissionEvaluator`

`shared-security` has to enforce `@RequirePermission`. The decision lives in
`permission`.

The obvious wiring — `shared-security` depending on `permission-application` and calling its
query port — is the one to avoid. It makes the platform unbuildable without a bounded
context, drags `permission` into any second application built on this platform, and creates
a latent cycle the moment `permission-infrastructure` needs anything from `shared-security`.

So `shared-authz` declares `PermissionEvaluator`, `PermissionQueryUseCase` extends it, and
Spring injects the context's bean into the platform's interceptor. The dependency runs
context → platform like every other one.

`platformDoesNotDependOnAnyModule` is what stops the shortcut being taken again.

### `CacheSpec`

The platform owns the single `CacheManager`; each context contributes a `CacheSpec`, and
cache names are prefixed with the context so two modules cannot collide.

A `CacheManager` bean per module does not work. Spring needs exactly one primary, so the
application either fails to start or — depending on resolution order. Starts with one
module's caches registered and the other's silently absent, and a `@Cacheable` naming a
missing cache throws at the first call rather than at start-up.

### `TranslationResolver` and `TranslationCatalogue`

Text is stored by the `translation` context, and the platform's `MessageResolver` needs to
read it — including on the error path, where every problem document resolves a key.

Same treatment: `shared-lang` declares `TranslationResolver`, `translation-infrastructure`
supplies the bean, and `TranslationCatalogue` runs the other way so each module declares its
own defaults without anything central to edit.

### Permissions are per context; roles are not

A role scope — `GLOBAL` against `PROJECT`, say — is per **resource**, not per bounded
context: it earns its place when a person can manage one project and merely watch another.
Freshly has no such resource — a measuring
station has no owner, a measurement has no team — so a scope discriminator would be a field
in the key of every authorization lookup for a requirement that does not exist.

Roles stay global and stay in Keycloak, because a role is a statement about a *person*
("is an administrator"), not about a module. Roles named per context —
`airquality-admin` beside `useraccess-admin` — would make giving somebody a job a walk
through six lists, where one `ADMIN` role holding permissions from six catalogues says the
same thing once.

The boundary that *is* per context is the permission. `PermissionScope` already carries `GLOBAL | RESOURCE`, so if a
resource with an owner ever appears, the place is prepared.

### `PermissionCatalogue`, the same shape in the other direction

Already worked this way. The platform owns the contract; each context declares its own
permissions; `permission` assembles the catalogues without compiling against any of them.
That is what let `common.enums.Permission` — one enum holding every context's
capabilities — be dismantled.

---

## What deliberately stays unshared

**Configuration properties.** `KeycloakProperties`, `AuthProperties` and `GiosProperties`
each live in the infrastructure of the one module that reads them. Held centrally they would
be reachable by every module, which puts the identity-provider client secret on the compile
path of code that draws charts.

`useraccess` declaring its own `KeycloakRealmProperties` over the same prefix as `auth`'s is
this rule working, not duplication to remove: the two modules read the provider for
different reasons and neither owns the other's configuration.

`CorsProperties` is the one exception, because CORS genuinely is a platform concern with
no owning context. `platformHoldsNoModuleConfiguration` allows exactly that one class.

**Error catalogues.** Each context has its own enum in `domain/error/`. Only `DomainError`
and `ErrorKind` are shared, and neither knows what any failure means.

**Permission catalogues.** Each context declares its own enum. The platform holds only the
contract.

**`ApplicationBeansConfig`.** Five near-identical files, deliberately not extracted. They
differ in exactly the thing that matters — which ports exist and which are transactional —
so a shared abstraction over them would be a configuration format for something that is
already configuration.

**Domain value objects.** `SignedInUser` is in `auth`. `Station` is in `airquality`. Two
contexts modeling a similar-sounding thing differently is the point of bounded contexts,
not a duplication to eliminate.

---

## Adding to the platform

1. Does more than one context need it *today*? If not, it belongs to the one that does.
2. Does it carry domain meaning? If yes, it belongs to a context — even if two of them
   want something similar.
3. Is it framework-free? If yes it may go in `shared-lang` or `shared-authz` and an
   application layer may name it. If not, it goes in `shared-infra`, `shared-web` or
   `shared-security` and only adapters may.
4. Would sharing it make the platform depend on a context? Then it is an SPI: the platform
   declares the interface, the context implements it.

`PlatformArchitectureTest` in `bootstrap` checks 3 and 4 mechanically, and part of 2. The
rest is review.
