# notification

Owns outgoing e-mail: the templates, their subjects, and the transport.

## What it owns

`EmailTemplate` and its subject line live here rather than with the caller. A module
that only wants to *ask for* mail should not be writing prose about sending it — so
`auth` asks for `EMAIL_VERIFICATION` and supplies the variables, and what the reader
sees in their inbox is this module's business.

That is also why the named methods (`sendEmailVerification`, `sendWelcomeEmail`,
`sendPasswordReset`) sit on the inbound port alongside the generic `send`. A caller
assembling `Map.of("username", …, "verificationLink", …)` itself would be encoding
this module's template contract at its own call site, and a renamed variable would
fail silently as a blank in somebody's inbox.

## Decisions worth knowing

### A failure is reported, not swallowed

`NotificationCommandService` records the failure on the aggregate and rethrows.

Swallowing would be tempting — a welcome e-mail that does not arrive is not worth
failing a registration over. It is wrong because the caller has the context to decide
and two callers decide differently: `auth` rolls back the Keycloak user when a
*verification* mail cannot be sent, because an account nobody can verify is worse
than no account. A welcome mail could be dropped. This module cannot tell those
apart.

### The subject is a key, the body is a template

Both are resolved against the recipient's locale: `EmailTemplate` carries
`email.verification.title` rather than an English sentence, and `SmtpEmailSender` resolves
it through the same `MessageSource` the Thymeleaf templates use for `#{...}`. A literal
subject would head a Polish message in English, and an administrator could not correct it.

### Two failures, two statuses

`SmtpEmailSender` distinguishes a transport refusal from a template that will not
render, and raises a different error for each.

Not cosmetic: SMTP being unreachable is transient and worth retrying, so it becomes a
502. A missing template or an unresolvable Thymeleaf expression will fail identically
on every attempt, so it becomes a 500. Collapsed together, a caller with a retry
policy hammers SMTP over a typo in a template file.

### Dispatch is an application port, not a domain one

`application/port/outbound/EmailDispatchPort`. Sending is not a domain concept —
nothing about an e-mail notification requires that a transport exists, and a domain
describing one is the same category error as a repository that mentions MongoDB.

Repository ports stay in the domain: those express what the domain needs to remember,
which is part of the model. Everything else outbound lives in the application layer.

### No transaction wrapper

Unlike `useraccess`, `ApplicationBeansConfig` here does not wrap the use case.
This module writes nothing, and wrapping it would suggest a rollback could unsend an
e-mail. It cannot — SMTP does not join a database transaction, and pretending
otherwise is how a retry sends twice.

### The aggregate is not persisted, and that is a known gap

`EmailNotification` has a full delivery state machine — `PENDING`, `SENT`, `FAILED`,
`sentAt`, `errorMessage` — and no document, no repository, and nothing that outlives
the use case call. The module's Gradle file even declared a MongoDB dependency that
nothing used.

So the delivery log the state machine describes does not exist; a failed send is
visible only in the application log. The state machine is kept rather than deleted
because it is the shape a log would need and because `markAsSent` is where "we
already sent this" would be caught. Adding the repository is a small change from
here.

This is written down so that nobody discovers the gap while looking for a bounce and
finding nothing.

### No query port

Nothing to query, because nothing is stored. An empty `NotificationQueryUseCase`
added for symmetry with the other modules would be a port, a service and a bean for
zero reads.
