# Queries

How user lists and lookups are read.

`UserAccessCommandUseCase` and `UserAccessQueryUseCase` are separate; there is no
separate read model. Every query here returns a single aggregate with nothing to
assemble across boundaries, so a projection would be a second place to maintain
for no measurable gain.

The split still earns its keep: `TransactionalUseCaseFactory` reads the
transaction mode from which port it is, so a method added to the query side cannot
accidentally run read-write.

Revisit this when a query has to span `useraccess` and `permission`.

## Paging happens in the database

The port takes a `PageRequest` and the adapter pages in MongoDB with an explicit sort.
Returning every user and letting the client cope is fine at a hundred users and nothing
beyond; and without the explicit sort, Mongo does not promise to return rows in the same
order twice, so page 2 can repeat a row from page 1.
