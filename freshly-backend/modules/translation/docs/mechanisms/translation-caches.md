# Translation caches

How resolving a key and serving a bundle stay fast.

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
[Translations](../../../../docs/mechanisms/translation-catalog.md).
