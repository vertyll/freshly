# Sign-in state

How a callback is matched to the sign-in that started it.

`HostedSignInRequests` builds the authorization request with Spring's resolver and PKCE, and
adds `ui_locales`, `prompt=create` and the allowed `kc_action`. Spring keeps the request in the
server-side session and the callback takes it out once: a callback whose state was not issued
to this browser is refused (`?error=state_mismatch`), so a code obtained elsewhere cannot be
planted in someone's session. Spring changes the session identifier at sign-in, so one known
before sign-in is worthless after it.
