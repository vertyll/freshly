# Cross-site requests

Why a forged request from another site cannot act with the user's session.

The session cookie is `SameSite=Lax`, which already keeps it off cross-site `POST`, `PUT`,
`PATCH` and `DELETE`. Lax still treats another subdomain of the same site as same-site, so
`SessionAccessTokenFilter` also reads `Sec-Fetch-Site`: an unsafe method sent from anywhere other
than this origin is treated as anonymous and answered 401. `POST /auth/logout` applies the same
rule and answers 403, so another page cannot sign the person out.
