# translation

Owns the text the application shows: what each module ships as a default, what an
administrator has changed, and which language a reader gets.

Full design notes in `docs/translations.md`; this covers what is specific to the module.

## Why this is a bounded context and not infrastructure

It has an aggregate with invariants, an error catalogue, an administration API and its own
persistence. "Look up a string" is the least interesting thing it does — the interesting part
is that a key has two independent sets of text with different lifecycles, and that a redeploy
must touch exactly one of them.

## The distinction everything rests on

| | Source | Overwritten at start-up |
|---|---|---|
| `defaults` | the owning module's `TranslationCatalogue`, in code | **yes, always** |
| `overrides` | an administrator, through the API | **never** |

Resolution is `override → default → the key itself`, in a language the request's locale has
already been narrowed to by `SupportedLocaleConfig` (English when nothing in `Accept-Language`
matches). A missing text shows as its key on purpose: `error.user.notFound` on a screen says
exactly what is missing and where to add it.

Merging the two maps is the obvious simplification and it destroys the feature. The first
time somebody fixes a typo in the UI and the next release silently reverts it, nobody edits
anything again. Keeping them apart also buys the other direction: improving the source text
in code still reaches everyone who has not overridden that key.

`TranslationRegistrationServiceTest` pins this at the orchestration level and
`TranslationKeyTest` at the aggregate level, because it can be lost in either.

## Decisions worth knowing

### Keys cannot be created through the API

They come from the code that reads them. One invented in an admin screen would be a string
nothing ever looks up. `TranslationRegistrationUseCase` is an inbound port with no HTTP
endpoint — its driving adapter is a start-up runner, which is a driving adapter exactly as a
controller is.

### One context may not declare another's key

`refreshDefaults` refuses it, and `ErrorMessageCoverageTest` fails the build first. Unchecked,
two modules would overwrite each other's text on alternate boots — a fault that presents as a
caching bug and takes a long time to trace.

### No fallback to another language

A key with no Polish text renders as the key, not as the English. A Polish reader silently
served English cannot tell that from a translation somebody chose; a raw key is visibly wrong
and names what to fix. Nor does a miss throw — on the error path that would bury the error
being reported under a 500.

### Staleness is reported, never corrected

An override records the default it was written against. When the module later changes that
default the override keeps winning — it must, or the edit was pointless — but
`GET /translations/stale` lists every override whose source has since moved. Without it, an
override written against "Delete account" keeps serving after the source becomes "Deactivate
account".

### The bundle lives under `/bundles/{language}`

Not at `/translations/{language}`. The bundle is public, so the filter chain permits its
path, and a single-segment variable there matches any single segment — including `stale`.
Naming the collection removes the overlap instead of leaving method security as the only
thing between an anonymous request and an admin endpoint.

### Two caches, not one

`StoredTranslationResolver` caches single keys and sits on the error path; every problem
document resolves one, so a round trip per refusal would make failing slower than succeeding.
`CachedTranslationBundles` caches whole bundles and sits on page load — without it a
conditional request would still load every key and hash them just to discover the client
already had them, and the 304 would cost more than the response it avoids.

Both evict wholesale on any edit. Computing which entries a changed key affects is more code
than refilling, and a stale entry means an administrator's correction does not appear.

Both are in-memory, so they are single-instance-correct only. A shared cache is a change to
the platform's `CacheConfig` alone.

### Registration is not transactional

One malformed key would otherwise roll back the whole registration and the application would
boot with no translations at all. Per-context failure is logged and the rest proceeds.

### Not multi-tenant

One set of overrides for the installation. Freshly has no notion of a tenant, and a
discriminator "in case" would put a field in the key of every lookup on the hot path for a
requirement that does not exist. If one appears, `TranslationKey`'s identity changes and the
collection needs a migration — the honest cost, written down.

## The spreadsheet

`GET /translations/export` and `POST /translations/import` exist for a translator who would
rather work through four hundred strings in one window than click through a screen.

Import treats a cell equal to the current default as *no override*, and clears one that
exists. Without that, downloading the file and re-uploading it unchanged would freeze the
whole catalogue as overrides, and every later improvement to the text in code would stop
reaching anyone.

Rows are validated, not trusted: a key no module declares is skipped, a pattern that will not
compile is rejected, and so is one whose placeholders differ from the default it replaces. The
rejections come back as codes with parameters, and the whole import is one transaction.

See `docs/translations.md` for the column layout and the report.
