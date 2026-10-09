# Contents

Every document in this repository, the module it belongs to, and what it covers. Terms are defined in
[GLOSSARY.md](GLOSSARY.md), and the specifications behind them are in [STANDARDS.md](STANDARDS.md).

## Start here

| Document                  | Module | Kind              | Covers                                                         |
|---------------------------|--------|-------------------|----------------------------------------------------------------|
| [freshly](README.md)      | —      | repository README | What the repository is, its stack and where to start.          |
| [Glossary](GLOSSARY.md)   | —      | reference         | Every term the docs use, and where it is explained.            |
| [Standards](STANDARDS.md) | —      | reference         | The RFCs and specifications the code implements or depends on. |

## Back-end

| Document                              | Module   | Kind          | Covers                                                             |
|---------------------------------------|----------|---------------|--------------------------------------------------------------------|
| [Back-end](freshly-backend/README.md) | back-end | module README | Layout, running it, the architecture checks and module boundaries. |

## Overview

| Document                                                         | Module   | Kind     | Covers                                                                                           |
|------------------------------------------------------------------|----------|----------|--------------------------------------------------------------------------------------------------|
| [Hexagonal Layering](freshly-backend/docs/hexagonal-layering.md) | back-end | overview | How the dependency rule is enforced in a single-process application, and what it cost.           |
| [Known limitations](freshly-backend/docs/known-limitations.md)   | back-end | overview | The limits of the design: single-instance caches, two systems per role change, one installation. |
| [Shared modules](freshly-backend/docs/shared-modules.md)         | back-end | overview | What lives in `platform/`, why each one is allowed to exist, and what may go in it.              |

## Mechanisms

| Document                                                                              | Module   | Kind      | Covers                                                                                                      |
|---------------------------------------------------------------------------------------|----------|-----------|-------------------------------------------------------------------------------------------------------------|
| [Error responses](freshly-backend/docs/mechanisms/error-responses.md)                 | back-end | mechanism | What the API answers when it refuses a request: a key for the client, and the text resolved for the reader. |
| [Translation catalog](freshly-backend/docs/mechanisms/translation-catalog.md)         | back-end | mechanism | Defaults and overrides, staleness, ICU patterns, keys nobody declares, the API.                             |
| [Translation migrations](freshly-backend/docs/mechanisms/translation-migrations.md)   | back-end | mechanism | The three changes to stored keys that registration cannot make on its own.                                  |
| [Translation spreadsheet](freshly-backend/docs/mechanisms/translation-spreadsheet.md) | back-end | mechanism | How the catalog travels to a translator and back without freezing defaults as overrides.                    |

## Modules

| Document                                                                                               | Module        | Kind          | Covers                                                                                       |
|--------------------------------------------------------------------------------------------------------|---------------|---------------|----------------------------------------------------------------------------------------------|
| [airquality](freshly-backend/modules/airquality/README.md)                                             | `airquality`  | module README | GIOŚ readings, statistics and rankings.                                                      |
| [GIOŚ integration](freshly-backend/modules/airquality/docs/mechanisms/gios-integration.md)             | `airquality`  | mechanism     | How GIOŚ's data and API reach the domain without its shape leaking in.                       |
| [Measurement sync](freshly-backend/modules/airquality/docs/mechanisms/measurement-sync.md)             | `airquality`  | mechanism     | How readings are pulled from GIOŚ and stored.                                                |
| [Proximity search](freshly-backend/modules/airquality/docs/mechanisms/proximity-search.md)             | `airquality`  | mechanism     | How "stations near me" is answered.                                                          |
| [auth](freshly-backend/modules/auth/README.md)                                                         | `auth`        | module README | Sign-in, tokens, sessions and refreshing.                                                    |
| [Account provisioning](freshly-backend/modules/auth/docs/mechanisms/account-provisioning.md)           | `auth`        | mechanism     | How a person gets an account the first time they sign in.                                    |
| [Cross-site requests](freshly-backend/modules/auth/docs/mechanisms/cross-site-requests.md)             | `auth`        | mechanism     | Why a forged request from another site cannot act with the user's session.                   |
| [Sign-in state](freshly-backend/modules/auth/docs/mechanisms/sign-in-state.md)                         | `auth`        | mechanism     | How a callback is matched to the sign-in that started it.                                    |
| [Token audience](freshly-backend/modules/auth/docs/mechanisms/token-audience.md)                       | `auth`        | mechanism     | Which access tokens the API accepts.                                                         |
| [Token refresh](freshly-backend/modules/auth/docs/mechanisms/token-refresh.md)                         | `auth`        | mechanism     | How the session keeps a valid access token without signing the user out when requests race.  |
| [permission](freshly-backend/modules/permission/README.md)                                             | `permission`  | module README | What each role may do.                                                                       |
| [Permission lookup](freshly-backend/modules/permission/docs/mechanisms/permission-lookup.md)           | `permission`  | mechanism     | How a permission check finds what a caller's roles grant, quickly and without a transaction. |
| [translation](freshly-backend/modules/translation/README.md)                                           | `translation` | module README | Shipped defaults and admin overrides.                                                        |
| [Translation caches](freshly-backend/modules/translation/docs/mechanisms/translation-caches.md)        | `translation` | mechanism     | How resolving a key and serving a bundle stay fast.                                          |
| [useraccess](freshly-backend/modules/useraccess/README.md)                                             | `useraccess`  | module README | Accounts and their roles.                                                                    |
| [Identity provider sync](freshly-backend/modules/useraccess/docs/mechanisms/identity-provider-sync.md) | `useraccess`  | mechanism     | What this module changes in Keycloak, and why Keycloak stays the source of truth.            |
| [Queries](freshly-backend/modules/useraccess/docs/mechanisms/queries.md)                               | `useraccess`  | mechanism     | How user lists and lookups are read.                                                         |
