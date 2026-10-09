# translation

Owns the text the application shows: what each module ships as a default, what an
administrator has changed, and which language a reader gets.

Full design notes in [Translations](../../docs/mechanisms/translation-catalog.md); this covers what is specific to the
module.

## Why this is a bounded context and not infrastructure

It has an aggregate with invariants, an error catalog, an administration API and its own
persistence. "Look up a string" is the least interesting thing it does — the interesting part
is that a key has two independent sets of text with different lifecycles, and that a redeploy
must touch exactly one of them.

## The distinction everything rests on

A key has two independent sets of text: `defaults` from the owning module's code, rewritten at every start-up, and
`overrides` from an administrator, never touched by a deployment. Why, and how a key resolves, is in
[Translations](../../docs/mechanisms/translation-catalog.md#the-two-layers).

`TranslationRegistrationServiceTest` pins this at the orchestration level and `TranslationKeyTest` at the aggregate
level, because it can be lost in either.

## Decisions worth knowing

### Keys cannot be created through the API

They come from the code that reads them. One invented in an admin screen would be a string
nothing ever looks up. `TranslationRegistrationUseCase` is an inbound port with no HTTP
endpoint — its driving adapter is a start-up runner, which is a driving adapter exactly as a
controller is.

## Mechanisms

- [Translation caches](docs/mechanisms/translation-caches.md) – How resolving a key and serving a bundle stay fast.
