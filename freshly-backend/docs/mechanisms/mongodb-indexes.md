# MongoDB indexes

How every declared index is created under its name, and an index an earlier release named is replaced.

A document declares its indexes where it declares its fields, with `@Indexed` and `@CompoundIndex`, and names each
one: `idx_<collection>_<field>…`, and `uk_<collection>_<field>…` for a unique one, so an error or a query plan that
names an index says which collection and fields it covers.

| Collection               | Index                                              | Keys                                 |
|--------------------------|----------------------------------------------------|--------------------------------------|
| `useraccess_system_user` | `idx_useraccess_system_user_is_active`             | `is_active`                          |
| `translation_key`        | `idx_translation_key_context`                      | `context`                            |
| `translation_key`        | `idx_translation_key_declared`                     | `declared`                           |
| `airquality_measurement` | `idx_airquality_measurement_station_id`            | `station_id`                         |
| `airquality_measurement` | `uk_airquality_measurement_station_id_measured_at` | `station_id`, `measured_at` (unique) |

## Created at start-up, not by the mapping context

Spring Data's `auto-index-creation` is off. It creates the indexes while the mapping context starts, before any of the
application's code runs, and MongoDB refuses to create an index whose keys an index of another name already covers.
Renaming an index would therefore stop the application from starting on a database an earlier release had indexed.

`MongoIndexes`, in `platform/shared-infra`, runs instead once every bean exists, before the server takes a request or a
scheduled task runs. For each `@Document` in the mapping context, and each index it declares, it drops an index on the
same keys under another name, then creates the declared one; creating an index that already exists is a no-op. A
database converges on what the code declares at every start, so, unlike a [translation
migration](translation-migrations.md), nothing is recorded: the translation migrations stay what they are, three verbs
over stored keys, and an index needs none of their bookkeeping.

A failure to create an index, such as a unique index over documents that break it, stops the start-up rather than
leaving the collection without it.

## The window of a rename

An index is dropped before its replacement is created, since MongoDB allows only one index on the same keys. For the
moment between the two the collection has no index on those keys, and while a unique index is renamed, nothing refuses
a duplicate. The collections are small and the sync takes a lock, so the window is short, and a duplicate written in it
makes the creation fail, and the start-up with it, instead of passing unnoticed.
