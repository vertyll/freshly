# Freshly — backend

Air quality monitoring. A **modular monolith**: one process, six bounded contexts, each
internally a hexagon.

## Layout

```
freshly-backend/
├── buildSrc/                    convention plugins + HexagonalClasspathCheck
├── platform/                    not bounded contexts
│   ├── shared-lang/             FRAMEWORK-FREE — errors, paging, VersionGuard, ports
│   ├── shared-authz/            FRAMEWORK-FREE — authorization published language + SPI
│   ├── shared-i18n/             FRAMEWORK-FREE — ICU message patterns
│   ├── shared-infra/            transaction proxy, logger adapter, cache manager
│   ├── shared-web/              problem details, ETag, paging response, annotations
│   ├── shared-security/         filter chain, method security, role extraction
│   └── shared-archunit/         the rules, as tests
├── modules/
│   ├── useraccess/              ← reference module; clone this shape
│   ├── notification/
│   ├── permission/
│   ├── airquality/
│   ├── translation/             editable UI text; see docs/translations.md
│   └── auth/
└── bootstrap/                   the only bootJar; owns application*.yml and i18n profiles: `local` (default) and `prod`
```

Each context is **three Gradle projects**, not one:

```
<context>-infrastructure ──► <context>-application ──► <context>-domain
```

## Running it

The profile is mandatory: `local` or `prod`. The permission seeder and the translation
registrar populate their collections on first boot.

**Mongo has to run as a replica set**, a single node included. Every inbound port goes through
a transaction, and multi-document transactions are not available on a standalone server — so a
standalone one starts and then fails at the first write. `docker-compose.dev.yml` starts it as
one, together with Keycloak (realm and client secrets from `docker/keycloak/realm-export.json`)
and maildev:

```bash
docker compose -f ../docker-compose.dev.yml up -d   # Mongo :27017, Keycloak :9000, maildev :1025/:1080
SPRING_PROFILES_ACTIVE=local ./gradlew :bootstrap:bootRun
```

`application-local.yml` holds every value the local profile needs; `application-prod.yml`
holds only `${...}` references to the environment.

## API documentation

```bash
./gradlew docs   # Javadoc of the platform libraries: build/docs/javadoc/index.html
```

The same site is published to GitHub Pages by `.github/workflows/javadoc.yml` on every push to `main`
that touches `platform/`.

## Verifying the architecture

```bash
./gradlew checkHexagonalDependencies   # framework on an inner classpath -> build fails
./gradlew checkArchitecture            # the above, plus every module's ArchUnit tests
./gradlew checkNullMarkedPackages      # a package carrying code but no @NullMarked
```

The first two overlap deliberately. The Gradle task reads the resolved `compileClasspath`
and catches frameworks arriving transitively; ArchUnit works at class level and catches what
a classpath cannot express — a `@Document` on a domain model, a port declared as a class,
one module reaching into another's internals.

The third guards a different kind of failure. NullAway checks exactly what JSpecify marks,
and a package does not mark its subpackages — so a package added without a
`package-info.java` is silently not checked, and the build stays green. See
`docs/open-items.md`.

See `docs/hexagonal-layering.md` for the rules and what the framework-free constraint
buys.

## Comments

`platform/` carries Javadoc and nothing else: it is a published language used by every
module, and a caller reads it without reading the implementation. Modules, `bootstrap` and the
build scripts carry no comments — a type, a method name and an error constant say what a
paragraph would repeat, and a paragraph that drifts is worse than no paragraph. Decisions the
code cannot state live in the module READMEs and `docs/`.

## Module boundaries

A module may reach another **only** through that module's inbound ports, and **only**
from its own infrastructure layer, where an anti-corruption adapter translates.

Today `auth` is the only context that reaches others — `useraccess` and `notification` —
via `auth-infrastructure/acl`. `AuthArchitectureTest` declares those neighbours and the
rules enforce the boundary.

## Where to start reading

| If you want to understand              | Read                                 |
|----------------------------------------|--------------------------------------|
| The shape every module follows         | `modules/useraccess/` and its README |
| Why the inner layers have no Spring    | `docs/hexagonal-layering.md`         |
| The hardest module                     | `modules/auth/README.md`             |
| What is shared, and why                | `docs/shared-modules.md`             |
| How text is stored and edited          | `docs/translations.md`               |
| What is not verified, and what is next | `docs/open-items.md`                 |

Every module has a README covering the decisions specific to it.
