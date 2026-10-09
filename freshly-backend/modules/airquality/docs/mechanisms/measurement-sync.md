# Measurement sync

How readings are pulled from GIOŚ and stored.

`AirQualitySyncScheduler` drives the sync; the conditional that disables it is a bean
condition on that adapter; the transaction is a decorator at the port. The service itself
holds no scheduling annotation and no reference to its own proxy.

It runs at `:40`. GIOŚ publishes the hour's readings shortly after the hour and computes the
index around `:35`; an earlier run would store this hour's readings beside last hour's
index.

## The sync use case is not wrapped in a transaction

Every other command port is. A sync run iterates a thousand stations with several HTTP
calls each; one transaction across that holds a connection for the whole run and means a
failure at station 900 discards the 899 measurements already gathered — the opposite of
the per-station isolation the use case is built around.

## One station's failure does not abandon the run

Each station is attempted independently and failures are counted into a `SyncReport`.
That is the opposite of sign-in in `auth`, and the difference is who can act: there the
caller has a session to revoke when provisioning fails, here nobody is waiting. The report also makes "every
station failed" distinguishable from a quiet success, which is the one case worth
paging someone for.

## Nulls are dropped, never stored as zero

GIOŚ returns null for every hour a sensor was down. A zero would read as perfectly clean
air, dragging every average toward a reassuring number precisely when the instrument
was not working.
