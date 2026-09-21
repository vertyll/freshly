# Open items

What is not verified, what is known to be incomplete, and what to do next. Everything here
describes the code as it stands.

---

## 1. What has and has not been verified

Nothing has been through `./gradlew build`. No Gradle daemon and no dependency resolution
were available while this was written.

**Verified**, with javac 21 and stubs for JSpecify and ICU:

- All 199 framework-free sources compile clean — `shared-lang`, `shared-authz`,
  `shared-i18n` and the domain and application layer of every context.
- No framework import in any domain or application source. The hexagonal rule holds in
  fact, not only in build configuration.
- Every internal import resolves to a type that exists.
- Every Gradle project reference resolves against `settings.gradle.kts`; every
  version-catalog reference exists; no build file sits outside the settings.
- Every outbound port has exactly one adapter; every inbound port has a bean.
- Cross-module imports are exactly three, all `auth.infrastructure -> *.application`.
- Every `DomainError` key and every permission description key resolves in both languages.
- Every package under `src/main/java` carries `@NullMarked`, and every package-info's
  declaration matches its position in the tree.

**Not verified**: the infrastructure layers, which need Spring, Keycloak, MapStruct,
MongoDB, ICU4J and POI on the classpath. Around 150 files have never seen a compiler.

The likely first failures, in rough order of probability:

- **Method signatures against the pinned library versions.** Every catalogue *alias*
  resolves, but nothing has checked that e.g. `UsersResource.searchByEmail(String, boolean)`
  exists in Keycloak 26.0.8 with that shape, or that
  `keycloak.realm(r).users().get(id).roles().realmLevel()` does.
- **ICU4J and Apache POI have never been resolved.** `icu4j = "76.1"` and `poi = "5.4.1"`
  are what this was written against, and the APIs used are long-stable
  (`MessageFormat.getArgumentNames`, `SXSSFWorkbook`, `DataFormatter`). `IcuMessages` and
  the two spreadsheet classes are the first things to compile.
- **`new MongoTransactionManager(MongoDatabaseFactory)`** in `bootstrap/TransactionConfig`.
  Stable since Spring Data MongoDB 2.1; if the pinned version moved to a factory method,
  this is where it shows.
- **`GiosRestClientConfig`.** `ClientHttpRequestFactories` has moved package more than once
  across Boot versions. Adjust to whatever the pinned one exposes.
- **`PathPatternRequestMatcher`** in `PublicEndpointRegistry` needs Spring Security 6.5+.
- **`lombok.copyableAnnotations` with a TYPE_USE annotation.** `lombok.config` asks Lombok
  to carry `@Nullable` onto generated members; recent Lombok does this for type-use
  annotations anyway, so the line is belt and braces rather than load-bearing.

**Expect NullAway to report real findings on that first build.** It runs at `ERROR` over
every package now, and the infrastructure layers have never been through it. The
`@Nullable` annotations added across the Mongo aggregation rows, the GIOŚ response
records and the two `callerId` helpers are what it would have found first; there will be
more.

---

## 2. Missing tests

Only domain and application unit tests exist, plus the ArchUnit rules. Missing:

- **`@SpringBootTest` in `bootstrap`** that starts the context. Seven
  `ApplicationBeansConfig` classes wire beans by hand, so a missing bean is a start-up
  failure rather than a compile error, and nothing catches it.
- **`@DataMongoTest` per persistence adapter**, particularly `AirQualityAnalyticsAdapter`,
  whose two aggregation pipelines are handwritten and entirely unverified.
- **`@WebMvcTest` per controller**, especially `AuthController`'s cookie handling and
  `TranslationSpreadsheetController`'s multipart binding.
- **The spreadsheet round trip.** `TranslationImportService` has unit tests covering every
  branch of the report, including the one that matters most — a cell equal to the default
  clears the override rather than storing it. What has no test is the file itself:
  `TranslationSpreadsheet` writing a workbook POI can read back. That wants a `@WebMvcTest`
  with a generated `.xlsx` fixture.
- **Testcontainers** for MongoDB and Keycloak.
- A test asserting every `PermissionCatalogue` bean is discovered and the seeder runs. The
  failure mode there is silent.

---

## 3. Known limitations

These are properties of the design as it stands, not defects to be repaired quietly.

### The caches are single-instance-correct only

`ConcurrentMapCache`, evicted locally, for both role grants and resolved translations. With
two instances, revoking a grant or correcting a translation on instance A leaves instance B
serving the old answer until it restarts.

A change to the platform's `CacheConfig` alone once there is a Redis or Hazelcast to point
at — but it is a correctness bug the moment the deployment scales, not a performance one.

### Assigning a role writes to two systems with no transaction across them

`replaceUserRoles` updates Keycloak and then the local projection, inside the database
transaction. If the commit fails after Keycloak succeeded, the person's real access has
changed and the copy this application lists has not.

The ordering makes the survivable case the common one — the provider is what a token
carries, so it is what matters, and the projection is only what the admin screen lists. The
way out is not a distributed transaction: it is reading roles from the provider when listing
a user and keeping no copy.

### `notification` stores nothing

`EmailNotification` has a full delivery state machine — `PENDING`, `SENT`, `FAILED`,
`sentAt`, `errorMessage` — and no document, no repository, nothing that outlives the use
case call. A failed send is visible only in the application log: no bounce record, no retry,
no way to answer "did this person ever receive their verification link".

### `deprovision` deactivates rather than deletes

`UserAccessCommandUseCase` exposes no delete, so a rolled-back registration leaves an
inactive `useraccess` record behind. The adapter reports that as success — the record is
no longer usable, which is what the compensation was for — but it is still there.

### No rate limiting

`/auth/*` accepts as many attempts as it is given. Deliberately out of scope here.

### One installation, one set of everything

No tenant discriminator anywhere: one set of translation overrides, one set of role grants,
one measurement history. Adding one later changes the identity of `TranslationKey` and
`RoleAuthority` and needs a migration of both collections.

---

## 4. Decisions left open

**`notification`: add the repository, or delete the state machine.** A model describing a
delivery log that does not exist is the worst of the three options, and it is what is there.

**`deprovision`: add a deleted to the `useraccess` command port, or provision *after* the
verification mail succeeds.** The second is better — it collapses `RegistrationService`'s
compensation stack to a single step and removes the only case where a failed cleanup can
leave an orphan.

---

## 5. What to do next, in order

1. **Get `./gradlew build` green.** Around 150 infrastructure files have never seen a
   compiler, and NullAway now runs at `ERROR` over all of them. Expect the first run to be
   long; expect most of what it says to be real.
2. **Add the `@SpringBootTest` and start the application.** Manual bean wiring means a
   missing bean is a start-up failure, and that is the fastest way to find the rest.
3. **Integration tests with Testcontainers**, persistence adapters first.
4. **Pin the Keycloak client secret in the realm import**, so local setup is reproducible.
5. **Settle the two decisions** in section 4.
6. **Operational gaps**: request id in MDC and in `ProblemDetail`, health indicators for
   Keycloak and GIOŚ, OpenAPI schemas for `problem+json` and the `fields` extension.

Nothing above is blocked by anything else. Items 1 and 2 are the only ones where "done"
currently means "compiles in my head".

---

## 6. Three things worth not doing

**Do not add a query port to `notification` for symmetry.** It stores nothing, so a read
side would have nothing behind it. Symmetry between modules is not a goal; each context's
shape should follow what it does.

**Do not extract the `ApplicationBeansConfig` classes into a shared abstraction.** They look
near-identical and are not. They differ in exactly the thing that matters — which ports
exist, which are transactional, which are deliberately not — so an abstraction over them
would be a configuration format for something that is already configuration. `auth`'s file
says why none of its use cases are wrapped; that sentence has nowhere to live in a generic
version.

**Do not merge `Email` from `notification` with anything in `auth`.** Two contexts modeling
a similar-sounding thing differently is the point of bounded contexts. One is a delivery
target, the other an identity attribute, and a merged type would have to satisfy both.
