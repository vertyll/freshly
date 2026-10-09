# translation

Owns the text the application shows: what each module ships as a default, what an
administrator has changed, and which language a reader gets.

Full design notes in [Translations](../../docs/translations.md); this covers what is specific to the module.

## Why this is a bounded context and not infrastructure

It has an aggregate with invariants, an error catalog, an administration API and its own
persistence. "Look up a string" is the least interesting thing it does — the interesting part
is that a key has two independent sets of text with different lifecycles, and that a redeploy
must touch exactly one of them.

## The distinction everything rests on

A key has two independent sets of text: `defaults` from the owning module's code, rewritten at every start-up, and
`overrides` from an administrator, never touched by a deployment. Why, and how a key resolves, is in
[Translations](../../docs/translations.md#the-two-layers).

`TranslationRegistrationServiceTest` pins this at the orchestration level and `TranslationKeyTest` at the aggregate
level, because it can be lost in either.

## Decisions worth knowing

### Keys cannot be created through the API

They come from the code that reads them. One invented in an admin screen would be a string
nothing ever looks up. `TranslationRegistrationUseCase` is an inbound port with no HTTP
endpoint — its driving adapter is a start-up runner, which is a driving adapter exactly as a
controller is.

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

The other decisions — no fallback to another language, staleness, the `/bundles/` path, one context per key,
non-transactional registration, no tenancy — and the spreadsheet import and export are in
[Translations](../../docs/translations.md).
