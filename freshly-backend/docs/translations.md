# Translations

Text is data, not a build artifact. Modules declare defaults in code; an administrator
overrides them at runtime; a redeployment never touches an override.

---

## Why the text is data

A `messages_*.properties` file per language holds every context's copy in one place: nobody
owns it, adding a key means editing a file five modules also touch, and — the point —
changing a sentence requires a deployment. A typo in a user-facing error sits there until the
next release.

Here the defaults are declared per module in code and stored, and an administrator's edit is
a separate layer on top. There are no properties files.

## The two layers

|             | Source                                                         | Overwritten at start-up |
|-------------|----------------------------------------------------------------|-------------------------|
| `defaults`  | the owning module's `TranslationCatalogue`, in code            | **yes, always**         |
| `overrides` | an administrator, through `PUT /translations/{key}/{language}` | **never**               |

Resolution is `override → default → the key itself`, in a language the request's locale has
already been narrowed to by `SupportedLocaleConfig` (English when nothing in `Accept-Language`
matches). A missing text shows as its key on purpose: `error.user.notFound` on a screen says
exactly what is missing and where to add it.

Collapsing these into one map is the obvious simplification, and it destroys the feature.
The first time somebody fixes a typo in the UI and the next release silently reverts it,
nobody edits anything again.

Keeping them apart also buys the other direction: improving the source text in code still
reaches everyone who has not overridden that key.

## Staleness

An override records the default it was written against.

When the module later changes that default, the override keeps winning — it must, or the
edit was pointless — but `GET /translations/stale` lists every override whose source has
since changed. Without it, an override written against "Delete account" keeps serving after
the source becomes "Deactivate account", and the two say different things with nobody the
wiser.

## The message format is ICU

Patterns are [ICU MessageFormat](https://unicode-org.github.io/icu/userguide/format_parse/messages/),
rendered by ICU4J, not by the JDK's `java.text.MessageFormat`.

Plurals are the reason. Polish selects between `one`, `few` and `many` by a rule on the last
digits — 1 stacja, 2 stacje, 5 stacji, 22 stacje — and the JDK's `ChoiceFormat` picks a branch
by numeric threshold, so the rule has to be written out by hand and is still wrong at 22. ICU
carries the CLDR rules:

```
Znaleziono {count, plural, one{# stację} few{# stacje} many{# stacji} other{# stacji}}.
```

Two more differences earn their keep.

**Arguments are named.** A translator editing `validation.size` sees `od {min} do {max}` and
does not have to work out which of two numbers `{0}` was. Under the JDK formatter the two were
told apart only by the iteration order of the constraint's attribute map, which is a way to
produce a sentence that is confidently wrong rather than obviously broken.

**An apostrophe only escapes before a brace.** `don't` survives. The JDK formatter renders it
as `dont`, silently, in exactly the languages that use apostrophes — which forces anything
built on it to skip formatting altogether when no arguments are passed.

`IcuMessages` in `platform/shared-i18n` is the one place ICU is named. It has three jobs:
compile-check a pattern, report the placeholder names it uses, and render it.

### Patterns are checked three times

| When                                        | By what                                      | On failure                                        |
|---------------------------------------------|----------------------------------------------|---------------------------------------------------|
| Build                                       | `ErrorMessageCoverageTest`                   | the build fails                                   |
| Start-up, as a module declares its defaults | `TranslationKey.declare` / `refreshDefaults` | that context's registration is logged and skipped |
| Save, from the admin screen or an import    | `TranslationKey.override`                    | 400, or a rejected row in the import report       |

An unbalanced brace does not fail quietly — it throws when the message is rendered, which is
usually on an error path, in a request nobody was looking at.

The build test also checks that a key uses **the same placeholders in every language**. The
call site passes one set of arguments; a Polish string naming `{limit}` where the English one
names `{max}` renders the placeholder literally, and only for Polish readers.

### An override may not change the placeholders

`TranslationKey.override` refuses text whose placeholders differ from the default it replaces.
Drop one and the caller's value never reaches the reader; invent one, and it renders as a
literal `{limit}`. Both look like the translation saved cleanly. Reordering is fine — that is
the point of naming them.

The rule lives in the aggregate rather than in a service, so the admin endpoint and the
spreadsheet import cannot disagree about it. The grammar itself arrives as a method argument
(`MessageGrammar`), which keeps ICU out of the domain while leaving the invariant where it
belongs.

## Spreadsheet import and export

For a translator who does not want an admin screen, and for reviewing a few hundred strings
at once.

`GET /translations/export` downloads it (`translations:read`) and `POST /translations/import` applies an edited copy
(`translations:edit`); both are in the Swagger UI.

The sheet is `key | module | en | pl`, one row per key, sorted by module then key. Cells carry
the **effective** text — the override if there is one, the default otherwise — and an
overridden cell is shaded, so a reviewer can see what has been edited without a second column
saying so. The header row and the key column are frozen.

**Language columns are headed by the tag, not by a language name.** A re-import reads the
column back by that heading, so it must not depend on who exported the file or in what
language they were working.

### What the import does with a cell

| Cell                                    | Outcome                                    |
|-----------------------------------------|--------------------------------------------|
| Blank                                   | ignored                                    |
| Equal to the current default            | the override is **cleared**, not rewritten |
| Equal to the current override           | unchanged                                  |
| Anything else                           | saved as an override                       |
| Not a compilable ICU pattern            | rejected, with the reason                  |
| Different placeholders from the default | rejected, with both sets                   |
| A key no module declares                | skipped, and listed                        |
| A language column that is not supported | skipped, and listed                        |

The second row is the one worth dwelling on. Export writes the effective text, so a file that
is downloaded and re-uploaded unchanged would otherwise turn **every default in the catalogue
into an override** in one request. From then on, improving the source text in code would
never reach anyone. Treating "same as the default" as "no override" is what makes a round trip
a no-op.

Nothing is applied halfway: the import runs in one transaction, and a rejected row is reported
rather than thrown, so one bad cell does not cost the other four hundred.

The report names rows by the number the spreadsheet shows, so a rejection points at a row the
person can go and look at. Rejections carry a `code` and its parameters rather than a
sentence, like every other error in this API, so an admin screen translates them the same way
it translates everything else.

```json
{
  "applied": 12,
  "cleared": 2,
  "unchanged": 391,
  "unknownKeys": ["error.auth.somethingRemoved"],
  "unknownLanguages": [],
  "rejected": [
    { "rowNumber": 57, "key": "validation.size", "language": "pl",
      "code": "error.translation.placeholderMismatch",
      "params": { "expected": "[max, min]", "actual": "[max]" } }
  ],
  "missing": [{ "key": "error.airquality.stationRetired", "language": "pl" }]
}
```

`missing` is computed after the import across the whole catalogue, not just the uploaded rows:
the question a translator is asking at that moment is what is still left to do.

**Keys still cannot be created this way.** A row whose key no module declares is reported and
skipped, exactly as an invented key would be refused by `PUT`.

## Migrations: the three things registration cannot do

Registration is declarative. A module states its defaults, and they are written from nothing
at every start-up, so adding and changing text needs no migration and never will.

Three operations are not like that, because they **transform** what is already stored:

|                              | Why registration cannot do it                                                |
|------------------------------|------------------------------------------------------------------------------|
| Rename a key                 | The old document stays behind, holding somebody's override                   |
| Retire a key                 | Nothing ever removes it                                                      |
| Move a key to another module | `refreshDefaults` refuses it — a key belongs to the context that declared it |

So there is a small ordered mechanism for exactly those, and nothing else:

```java
@Component
class RenameTokenExpired implements TranslationMigration {
    public String id()      { return "2026-09-auth-rename-token-expired"; }
    public String context() { return "auth"; }

    public void apply(TranslationMigrationOperations operations) {
        operations.rename("error.auth.tokenExpired", "error.auth.linkExpired");
    }
}
```

Declared by the module that owns the keys, discovered like its `TranslationCatalogue`,
recorded by id in `translation_migration`, and run **before** the catalogue registrar. A
rename moves the override to a name the module is about to declare, and the other order
would leave the two disagreeing for one boot.

Each runs in its own transaction, and the id is recorded inside it, so a migration that
threw halfway is retried whole rather than remembered as done. Ids sort, which is what makes
the order the same on every instance; bean discovery order is not.

`rename` carries overrides but not defaults — the owning module writes those moments later —
and an override already on the target wins, because the target is the name in use. It is a
no-op when the old key is gone, so a second instance starting does not fail.

**A fourth verb that writes text would be a mistake.** The same sentence would then live in
a migration and in a catalogue, and which one is current would depend on the order things
ran. That is the drift the defaults/overrides split exists to prevent.

## Keys nobody declares any more

After every catalogue has registered, whatever is stored and was not declared is marked an
orphan, and `GET /translations/orphans` lists it.

Marked, not deleted: the defaults are gone but somebody's override may not be, and deleting
it would throw away work to save a row. Retiring a key is a deliberate act — that is what
the migration is for.

The sweep is skipped when any catalogue failed to register, because marking then would call
every key of the failed module an orphan and put it in front of an administrator as text
nobody ships any more.

One key being refused does not cost its module the other forty: registration collects
refusals per key, logs each, and saves the rest. The usual cause of a refusal is a key that
moved between modules, which is the migration above.

## Shape

```
platform/shared-i18n/
└── IcuMessages             validate, placeholders, render — the only place ICU is named

platform/shared-lang/i18n/
├── TranslationCatalogue            SPI — a module declares its defaults
├── TranslationMigration            SPI — a module declares a one-off change
├── TranslationMigrationOperations  rename, retire, reassign
└── TranslationResolver             SPI — the platform asks for one key

platform/shared-web/i18n/
├── MessageResolver         what application code uses
├── TranslationMessageSource  answers anything that asks Spring for a MessageSource
└── PlatformTranslationCatalogue  the platform's own keys

modules/translation/
├── translation-domain/          TranslationKey, SupportedLanguage, MessageGrammar, TranslationError
├── translation-application/     bundle, resolve, list, override, register, import, export
└── translation-infrastructure/  Mongo, controllers, catalogue registrar, resolver,
                                 ICU grammar adapter, POI spreadsheet
```

The platform does not depend on the module. It declares `TranslationResolver`;
`StoredTranslationResolver` in `translation-infrastructure` supplies the bean. The same
inversion as `PermissionEvaluator` and `CacheSpec`, and `platformDoesNotDependOnAnyModule`
enforces it.

Each context declares its own keys in its own `*TranslationCatalogue`, exactly as it
declares its own permissions. Adding a key touches one module.

## The API

The endpoints are in the Swagger UI. Three things about them are not visible there:

**The bundle is public and has to be.** The sign-in page needs its labels before anyone has
signed in.

**The ETag is not decoration.** A bundle is a few hundred keys and every page load needs all
of them. With revalidation the second request onward is an empty 304 until somebody edits
something; without it, this feature is a tax on every request. The tag is a hash of the
resolved content, not a timestamp — so it changes when the text does and not when a redeployment
rewrites identical defaults.

**The bundle sits under `/bundles/`, not at `/translations/{language}`.** The endpoint is
public, so whatever path it has is what the filter chain permits — and a single-segment
variable matches any single segment, including `stale`. At the shorter path,
`GET /translations/stale` would reach the filter chain unauthenticated; method security would
still refuse it, but that leaves one layer where the design intends two.
`PublicEndpointRegistry` cannot catch this, because a pattern is correct in isolation and
wrong beside its siblings. A public path with a variable segment is worth a second look.

Keys cannot be created through the API at all; see
[the module](../modules/translation/README.md#keys-cannot-be-created-through-the-api).

## Decisions worth knowing

**No fallback to another language.** A key with no Polish text renders as the key, not as
the English. A Polish reader silently served English cannot tell that from a translation
somebody chose; a raw key is visibly wrong and names what to fix. Nor does a miss throw — on
the error path that would bury the error being reported under a 500.

**Registration runs on every start-up.** Same reasoning as `PermissionSeeder`: a module
deployed after the store was first populated would otherwise never register, and its keys
would resolve to themselves forever with nothing reporting it. Safe only because
registration touches `defaults` alone.

**One context may not declare another's key.** `TranslationKey.refreshDefaults` refuses it,
and `ErrorMessageCoverageTest` fails the build first. Unchecked, two modules would overwrite
each other's text on alternate boots — a fault that presents as a caching bug.

**Registration is not transactional.** One malformed key would otherwise roll back the whole
registration and the application would boot with no translations at all. Per-context failure
is logged and the rest proceeds.

**Two caches, single-instance-correct.** Keys and whole bundles are cached in memory and evicted wholesale on any
edit; why two, and why that is safe only on one instance, is in
[the module](../modules/translation/README.md#two-caches-not-one).

**`detail` stays in problem documents.** Omitting it and making the client resolve `code`
itself would be defensible if the text lived behind another network call. It does not: the
store is one in-process call away and the request carries an `Accept-Language`, so
withholding the prose would make every client redo work the server has already done — on the
error path, which is exactly when things are already going wrong. `code` is still present, so a client that wants
its own wording ignores `detail`.

**Not multi-tenant.** One set of overrides for the installation. Adding a discriminator "in
case" would put a field in the key of every lookup on the hot path for a requirement that
does not exist. If it ever does, `TranslationKey`'s identity changes and the collection needs
a migration — the honest cost, written down.

## Field validation carries keys too

Left to Hibernate Validator, a rejected field answers with its default message —
`"size must be between 3 and 50"`. English regardless of `Accept-Language`, absent from the
store so nobody can edit it, and unbranchable without matching prose against it. Half the API
would return keys and half sentences.

Each rejected field carries a key instead:

```json
{
  "type":   "urn:freshly:error:error.common.validationFailed",
  "status": 400,
  "code":   "error.common.validationFailed",
  "fields": [
    { "field": "username", "code": "validation.size",
      "message": "Musi mieć od 3 do 50 znaków.", "params": { "min": 3, "max": 50 } },
    { "field": "email", "code": "validation.email",
      "message": "To nie jest poprawny adres e-mail.", "params": {} }
  ]
}
```

**Positional arguments and named ones cannot be mixed.** ICU renders a pattern either way,
but not both: a pattern written with `{min}` cannot be rendered with a positional argument
list, and the render fails quietly back to the raw pattern. Application code goes through
`MessageResolver`, which passes named arguments, so it does not arise there. A caller that
reaches `TranslationMessageSource` with positional arguments against a named-argument default
would print the pattern, braces and all, with nothing in the log. Keep such a key's
placeholders positional.

**Keys are derived from the constraint type, not written on the annotation.** The obvious
approach is `@Size(message = "{validation.username.tooLong}")`, and it permanently excludes
that text from the store: Hibernate Validator resolves `{...}` through its own
`ResourceBundle`, not Spring's `MessageSource`. It also means a key per field per
constraint, hundreds of them, each written by hand.

Deriving gives one key per constraint kind interpolated with the constraint's own
attributes. `validation.size` with `min` and `max` covers every `@Size` in the application.

The cost is that a field cannot have bespoke wording. If one ever needs it, the fix is a key
derived from the field name falling back to the generic one — not a message attribute.

`params` travels alongside so a client can render its own sentence, and so a translation
interpolates the numbers rather than hard-coding them.

No annotation carries a `message` attribute, so there is no
`ValidationMessages_*.properties` to keep in step with anything.
