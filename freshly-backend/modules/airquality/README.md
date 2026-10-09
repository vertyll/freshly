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

## Structure worth knowing

### Repository and analytics are separate ports

`calculateStatistics` and `getRanking` live on `AirQualityAnalyticsPort`, not on
`AirQualityHistoryRepository`. They are backed by Mongo aggregation pipelines, which is
the right implementation — averaging two thousand documents in Java to produce six
numbers is not an improvement in purity.

Keeping them off the repository keeps it fakeable as a map: a test about saving should
not have to supply a ranking algorithm.

### Reads are public, and say so

Government public-health data. An endpoint left simply unguarded looks identical to an
oversight; `@PublicEndpoint` states the decision. Only `sync` and `purge` carry
permissions.

## Mechanisms

- [GIOŚ integration](docs/mechanisms/gios-integration.md) – How GIOŚ's data and API reach the domain without its shape
  leaking in.
- [Measurement sync](docs/mechanisms/measurement-sync.md) – How readings are pulled from GIOŚ and stored.
- [Proximity search](docs/mechanisms/proximity-search.md) – How "stations near me" is answered.
