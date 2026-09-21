rootProject.name = "freshly"

include(
    // Framework-free. An application or domain layer may depend on these two and
    // nothing else; `checkHexagonalDependencies` enforces it.
    "platform:shared-lang",
    "platform:shared-authz",
    "platform:shared-i18n",

    // Spring-bound. Only `*-infrastructure` and `bootstrap` may depend on these.
    "platform:shared-infra",
    "platform:shared-web",
    "platform:shared-security",

    // Test-only.
    "platform:shared-archunit"
)

// ---------------------------------------------------------------------------
// Bounded contexts.
//
// Each is three Gradle projects, not one. The dependency rule
// (infrastructure -> application -> domain) is a build fact here, not a naming
// convention: `useraccess-domain` cannot see `useraccess-infrastructure`
// because nothing declares that edge.
// ---------------------------------------------------------------------------
include(
    "modules:useraccess:useraccess-domain",
    "modules:useraccess:useraccess-application",
    "modules:useraccess:useraccess-infrastructure",

    "modules:notification:notification-domain",
    "modules:notification:notification-application",
    "modules:notification:notification-infrastructure",

    "modules:permission:permission-domain",
    "modules:permission:permission-application",
    "modules:permission:permission-infrastructure",

    "modules:airquality:airquality-domain",
    "modules:airquality:airquality-application",
    "modules:airquality:airquality-infrastructure",

    "modules:translation:translation-domain",
    "modules:translation:translation-application",
    "modules:translation:translation-infrastructure",

    "modules:auth:auth-domain",
    "modules:auth:auth-application",
    "modules:auth:auth-infrastructure"
)

// ---------------------------------------------------------------------------
// The only project that produces a runnable jar.
//
// One process means one composition root. Every `*-infrastructure` is a plain library and
// `bootstrap` is the only project that produces a runnable jar.
// ---------------------------------------------------------------------------
include("bootstrap")
