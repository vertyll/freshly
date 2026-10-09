# Translation migrations

The three changes to stored keys that registration cannot make on its own.

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
recorded by id in `translation_migration`, and run **before** the catalog registrar. A
rename moves the override to a name the module is about to declare, and the other order
would leave the two disagreeing for one boot.

Each runs in its own transaction, and the id is recorded inside it, so a migration that
threw halfway is retried whole rather than remembered as done. Ids sort, which is what makes
the order the same on every instance; bean discovery order is not.

`rename` carries overrides but not defaults — the owning module writes those moments later —
and an override already on the target wins, because the target is the name in use. It is a
no-op when the old key is gone, so a second instance starting does not fail.

**A fourth verb that writes text would be a mistake.** The same sentence would then live in
a migration and in a catalog, and which one is current would depend on the order things
ran. That is the drift the defaults/overrides split exists to prevent.
