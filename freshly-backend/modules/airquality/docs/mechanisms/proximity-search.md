# Proximity search

How "stations near me" is answered.

`StationCatalogueAdapter` caches the station list through a `StationCache` bean so the
caching proxy is actually in the path. The sync reads stations from the same catalog, not
from GIOŚ, and the cache is dropped once a day: GIOŚ publishes the list yearly, and the
station-list endpoint may be limited to two requests a minute.

The scan over it is linear, deliberately: a thousand distance calculations in memory is well
under a millisecond, and a geospatial index would mean persisting the station list and
keeping it in step with GIOŚ. The port allows that later without any caller changing.
