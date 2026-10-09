# Error responses

What the API answers when it refuses a request: a key for the client, and the text resolved for the reader.

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
