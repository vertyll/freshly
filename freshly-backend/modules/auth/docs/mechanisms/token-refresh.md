# Token refresh

How the session keeps a valid access token without signing the user out when requests race.

Keycloak rotates refresh tokens and refuses a reused one, so two requests refreshing the same
session at once would sign the person out. `SingleFlightRefreshTokenProvider` wraps Spring's
refresh provider and runs a single refresh per refresh token: a second request with the same
token waits for the first and receives its result, and for thirty seconds afterwards a request
still holding the old token receives the same result instead of reaching Keycloak. A refresh
Keycloak refuses ends the session and is not remembered; one that fails because Keycloak is
unreachable leaves the session for the next request.

Replicas agree through Redis (`SharedRefreshes`). After the in-process single flight, a replica
claims `freshly:refresh-lock:<sha256 of the refresh token>` for ten seconds, calls Keycloak and
leaves the new tokens under `freshly:refresh-result:<sha256>` for thirty seconds; a replica that
finds the lock taken waits for that result instead of presenting the token again. A refused
refresh releases the lock and is not shared. When Redis is unreachable a replica refreshes on its
own, so Redis never becomes a reason a request fails.

## Sessions live in Redis

Spring Session keeps them under the `freshly:session` key namespace, so the application holds
no state of its own: a restart signs nobody out and replicas can be added.
