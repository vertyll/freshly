# Open items

What is known to be incomplete, the limits of the design, and what to do next. Everything here
describes the code as it stands.

---

## 1. Tests

**In place**: domain and application unit tests in every context, the ArchUnit rules and the
`checkHexagonalDependencies` and `checkNullMarkedPackages` tasks, adapter tests for the Keycloak
token issuer, and two `@SpringBootTest`s on MongoDB and Redis containers —
`FreshlyApplicationTest` starts the whole context and checks health and the public endpoints,
`HostedSignInTest` runs the sign-in flow end to end.

**Missing**:

- **`@DataMongoTest` per persistence adapter**, `AirQualityAnalyticsAdapter` first: its two
  aggregation pipelines are handwritten and only exercised through the application tests.
- **`@WebMvcTest` per controller**, especially `TranslationSpreadsheetController`'s multipart
  binding.
- **The spreadsheet round trip.** `TranslationImportService` has unit tests for every branch of
  the report; what has no test is the file itself — `TranslationSpreadsheet` writing a workbook
  POI can read back.
- **The Keycloak admin adapters** against a real Keycloak in a container.
- A test asserting every `PermissionCatalogue` bean is discovered and the seeder runs. The
  failure mode there is silent.

---

## 2. Known limitations

These are properties of the design, not defects to be repaired quietly.

### The caches are single-instance-correct only

`ConcurrentMapCache`, evicted locally, for both role grants and resolved translations. With
two instances, revoking a grant or correcting a translation on instance A leaves instance B
serving the old answer until it restarts. Redis is already part of the deployment for
sessions, so the fix is a change to the platform's `CacheConfig` — and it is a correctness
bug the moment the deployment scales, not a performance one.

### Assigning a role writes to two systems with no transaction across them

`replaceUserRoles` updates Keycloak and then the local projection, inside the database
transaction. If the commit fails after Keycloak succeeded, the person's real access has
changed and the copy this application lists has not.

The ordering makes the survivable case the common one — the provider is what a token
carries, so it is what matters, and the projection is only what the admin screen lists. The
way out is not a distributed transaction: it is reading roles from the provider when listing
a user and keeping no copy.

### One installation, one set of everything

No tenant discriminator anywhere: one set of translation overrides, one set of role grants,
one measurement history. Adding one changes the identity of `TranslationKey` and
`RoleAuthority` and needs a migration of both collections.

---

## 3. Decisions left open

**Accounts deleted in Keycloak.** Someone who deletes their account on Keycloak's pages
leaves a `useraccess` record behind. Nothing reads it again, but it is still there; listening
to Keycloak's admin events, or a periodic reconciliation, would remove it.

---

## 4. What to do next

1. **Persistence adapter tests** with Testcontainers, `AirQualityAnalyticsAdapter` first.
2. **Controller tests**, with the spreadsheet round trip.
3. **Caches in Redis**, before a second replica.
4. **Settle the decision** in section 3.
5. **Operational gaps**: a request id in the MDC and in `ProblemDetail`, health indicators for
   Keycloak and GIOŚ, OpenAPI schemas for `problem+json` and the `fields` extension.

Nothing above is blocked by anything else.

---

## 5. Worth not doing

**Do not extract the `ApplicationBeansConfig` classes into a shared abstraction.** They look
near-identical and are not. They differ in exactly the thing that matters — which ports
exist, which are transactional, which are deliberately not — so an abstraction over them
would be a configuration format for something that is already configuration. `auth`'s
use case talks only to Keycloak and through a port to `useraccess`, so it is not wrapped in a
transaction; that decision has nowhere to live in a generic version.
