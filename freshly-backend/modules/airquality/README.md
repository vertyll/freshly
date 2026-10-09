# airquality

Pulls readings from GIOŚ, stores them, and answers questions about them. The largest domain
in the application.

## The model

### `AirQualityMeasurement` is immutable

Private constructor, `record` / `reconstitute` factories, no setters. A historical reading is
the one kind of record that must never change, and identity is **station plus timestamp**
rather than the Mongo id — which is what makes "have we already stored this" answerable, and
what the unique index enforces.

### Readings are a map, not six nullable fields

`Map<Pollutant, Double>` rather than `pm10Value`, `pm25Value`, `so2Value` and their
siblings. A reading for a code the application does not track is visible instead of silently
vanishing, and adding a pollutant is one enum constant.

### The domain does not speak Polish

Everything GIOŚ-shaped — Polish keys, sensor codes, the index scale, Warsaw wall-clock
timestamps — is translated in one place, `GiosTranslator` in `infrastructure/gios`. It is the
translator of an anti-corruption layer: no HTTP, so it is tested as plain code in
`GiosTranslatorTest`, while `GiosAirQualityAdapter` is left with transport — paging, pacing,
the retry after a 429. The response records in `GiosResponses` are package-private, so
nothing of GIOŚ's shape reaches a port.

`Pollutant` is a plain enum. GIOŚ's codes, `"PM2.5"` among them, live in the translator's
table, and a code missing from it (`NOx`, `C6H6`) is a pollutant the domain does not chart.

Levels are read from the index **value**, 0 to 5, not from the Polish category name beside
it. The value is the official scale; the name is display text, with diacritics GIOŚ does not
compose consistently. `-1` or null is GIOŚ saying it computed no index, and reads as no
level; any other value off the scale fails the station rather than passing for one.

### Timestamps carry a zone

GIOŚ sends `2026-09-13 14:00:00` meaning Europe/Warsaw wall-clock, resolved
in `GiosTranslator`; everything above it is `Instant`. Parsed as `LocalDateTime` and stored,
measurements taken inside daylight saving would sit an hour away from those taken outside it,
and the repeated hour each October would produce two that look simultaneous.

### Bounds are value objects

`AnalysisWindow`, `SearchRadius` and `RankingLimit` rather than constants clamped inline at
each call site.

`AnalysisWindow` offers both `ofDays`, which clamps, and `ofDaysStrict`, which refuses,
because those are different situations: a UI slider should be clamped, while an API client
asking for 500 days should be told the ceiling rather than handed 90 days labeled as what
it asked for.

### The statistics round the unreassuring way

`dominantLevel()` returns `Optional.empty()` when there is no data — a station with nothing
recorded must not report the worst possible air quality — and a tie goes to the **worse**
level. A health-facing number should not round in the reassuring direction.

### Proximity search does not call GIOŚ

`StationCatalogueAdapter` caches the station list through a `StationCache` bean so the
caching proxy is actually in the path. The sync reads stations from the same catalog, not
from GIOŚ, and the cache is dropped once a day: GIOŚ publishes the list yearly, and the
station-list endpoint may be limited to two requests a minute.

The scan over it is linear, deliberately: a thousand distance calculations in memory is well
under a millisecond, and a geospatial index would mean persisting the station list and
keeping it in step with GIOŚ. The port allows that later without any caller changing.

### Scheduling is an adapter

`AirQualitySyncScheduler` drives the sync; the conditional that disables it is a bean
condition on that adapter; the transaction is a decorator at the port. The service itself
holds no scheduling annotation and no reference to its own proxy.

It runs at `:40`. GIOŚ publishes the hour's readings shortly after the hour and computes the
index around `:35`; an earlier run would store this hour's readings beside last hour's
index.

## Structure worth knowing

### Repository and analytics are separate ports

`calculateStatistics` and `getRanking` live on `AirQualityAnalyticsPort`, not on
`AirQualityHistoryRepository`. They are backed by Mongo aggregation pipelines, which is
the right implementation — averaging two thousand documents in Java to produce six
numbers is not an improvement in purity.

Keeping them off the repository keeps it fakeable as a map: a test about saving should
not have to supply a ranking algorithm.

### The sync use case is not wrapped in a transaction

Every other command port is. A sync run iterates a thousand stations with several HTTP
calls each; one transaction across that holds a connection for the whole run and means a
failure at station 900 discards the 899 measurements already gathered — the opposite of
the per-station isolation the use case is built around.

### One station's failure does not abandon the run

Each station is attempted independently and failures are counted into a `SyncReport`.
That is the opposite of sign-in in `auth`, and the difference is who can act: there the
caller has a session to revoke when provisioning fails, here nobody is waiting. The report also makes "every
station failed" distinguishable from a quiet success, which is the one case worth
paging someone for.

### Nulls are dropped, never stored as zero

GIOŚ returns null for every hour a sensor was down. A zero would read as perfectly clean
air, dragging every average toward a reassuring number precisely when the instrument
was not working.

### Reads are public, and say so

Government public-health data. An endpoint left simply unguarded looks identical to an
oversight; `@PublicEndpoint` states the decision. Only `sync` and `purge` carry
permissions.

## The GIOŚ API

Version 1 of the public API, `https://api.gios.gov.pl/pjp-api/v1/rest`.

The responses are JSON-LD with Polish keys — `"Identyfikator stacji"`, `"WGS84 φ N"`,
`"Lista danych pomiarowych"` — mapped verbatim in `GiosResponses`. Lists come wrapped and paged:
stations at up to 500 a page, readings at 24 (a day, newest first). `GiosAirQualityAdapterTest`
runs against recorded v1 responses, so a change to the format fails there rather than in a
sync.

| Data                                       | Limit GIOŚ publishes                                               |
|--------------------------------------------|--------------------------------------------------------------------|
| readings (`/data/getData`)                 | 1500 requests a minute                                             |
| index (`/aqindex/getIndex`)                | 1500 requests a minute                                             |
| stations and sensors                       | "2 and 1500 requests a minute", without saying which applies where |
| archive, statistics, exceedances, metadata | 2 requests a minute                                                |

Readings and index update hourly and reach three days back; anything older is only in the
archive. The terms also ask that the same data be fetched no more than twice an hour, which
one hourly run keeps to.

A run is about two thousand requests. The adapter spaces them at least 60 ms apart, which
caps it near a thousand a minute, and answers a 429 by waiting the `Retry-After` it is given
— at most a minute — and trying once more.

### Terms of use

Free, commercial use included: the dataset is published under CC BY 4.0 and the portal's
terms state that use is free of charge. Two obligations come with it, and both land on
whatever displays the data rather than on this backend:

- **Attribution**: `Źródło danych: GIOŚ - EKOINFONET`.
- **A note that the data was processed.** Statistics, rankings and the stored history are
  derived from GIOŚ's readings, and the terms require saying so when processed data is
  republished.
