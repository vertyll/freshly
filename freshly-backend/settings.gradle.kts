rootProject.name = "freshly"

include(
    "platform:shared-lang",
    "platform:shared-authz",
    "platform:shared-i18n",

    "platform:shared-infra",
    "platform:shared-web",
    "platform:shared-security",

    "platform:shared-archunit"
)

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

include("bootstrap")
