# Identity provider sync

What this module changes in Keycloak, and why Keycloak stays the source of truth.

`PUT /users/{id}/roles` goes through `RoleDirectoryPort`: the names are checked against the
realm, the provider is updated, and only then is the local copy saved. That copy is a
projection for reading and listing; it is not what is enforced.

Writing role names onto the local document and stopping there changes nothing that matters.
Authorization reads roles out of the token, and a token carries Keycloak's realm roles — the
administrator assigns a role, the screen shows it, and access stays as it was. A name with a
typo would be accepted just as readily.

`GET /roles` lists what the realm offers, so a panel has something to choose from rather than
a free-text box.

The order inside the use case is deliberate: the aggregate's own rule runs first, so an empty
set costs no round trip; then the realm check; then the provider; then the save. A failure at
any step leaves the previous ones either undone or harmless — the local write is last, and it
is the only one inside the transaction.

## Deactivation reaches the identity provider

`PATCH /users/{id}/deactivate` and `/activate` go through `RoleDirectoryPort.setEnabled` as well
as the aggregate. Sign-in happens on Keycloak's pages, which never consult this module, so a
deactivation recorded only here would leave the person able to sign in and holding valid
tokens. Disabling the Keycloak account also ends its sessions. The order is the same as for
roles: the aggregate's rule, then the provider, then the save.
