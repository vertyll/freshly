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
└── bootstrap/                   the only bootJar; owns application*.yml and i18n
                                 profiles: `local` (default) and `prod`
```

Each context is **three Gradle projects**, not one:

```
<context>-infrastructure ──► <context>-application ──► <context>-domain
```

## Running it

The database is greenfield — this has never been deployed, so there is nothing to migrate.
Start Mongo and Keycloak, drop whatever is in the local database, and let the application
create it: the permission seeder and the translation registrar populate their collections on
first boot.

**Mongo has to run as a replica set**, a single node included. Every inbound port goes through
a transaction, and multi-document transactions are not available on a standalone server — so a
standalone one starts and then fails at the first write, which is the more confusing of the two
failures. Locally:

```bash
mongod --replSet rs0 --dbpath ...        # then once, in mongosh:
rs.initiate()
```

One thing needs setting by hand. `docker/keycloak/realm-export.json` ships its client
secrets masked, so Keycloak generates a fresh one on import:

```bash
export APP_KEYCLOAK_ADMIN_SECRET=...   # from the Keycloak admin console
./gradlew bootRun
```

Pinning a fixed literal in the realm import removes that step; see the note at the bottom of
`bootstrap/src/main/resources/application-local.yml`.

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

`platform/` carries Javadoc: it is a published language used by every module, and a caller
reads it without reading the implementation. Modules carry none — a type, a method name and
an error constant say what a paragraph would repeat, and a paragraph that drifts is worse
than no paragraph. The exception is a decision the code cannot state, and that is a single
`//` line at the point where the decision is made.

## Module boundaries

A module may reach another **only** through that module's inbound ports, and **only**
from its own infrastructure layer, where an anti-corruption adapter translates.

Today `auth` is the only context that reaches others — `useraccess` and `notification` —
via `auth-infrastructure/acl`. `AuthArchitectureTest` declares those neighbours and the
rules enforce the boundary.

## Where to start reading

| If you want to understand | Read |
|---|---|
| The shape every module follows | `modules/useraccess/` and its README |
| Why the inner layers have no Spring | `docs/hexagonal-layering.md` |
| The hardest module | `modules/auth/README.md` |
| What is shared, and why | `docs/shared-modules.md` |
| How text is stored and edited | `docs/translations.md` |
| What is not verified, and what is next | `docs/open-items.md` |

Every module has a README covering the decisions specific to it.
