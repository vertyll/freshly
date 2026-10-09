# Translation spreadsheet

How the catalog travels to a translator and back without freezing defaults as overrides.

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

## What the import does with a cell

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
is downloaded and re-uploaded unchanged would otherwise turn **every default in the catalog
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

`missing` is computed after the import across the whole catalog, not just the uploaded rows:
the question a translator is asking at that moment is what is still left to do.

**Keys still cannot be created this way.** A row whose key no module declares is reported and
skipped, exactly as an invented key would be refused by `PUT`.
