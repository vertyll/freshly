package com.vertyll.freshly.airquality.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.airquality.application.port.outbound.AirQualityProviderPort;
import com.vertyll.freshly.airquality.domain.model.Station;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StationCache {
    public static final String STATIONS_CACHE = "airquality-gios-stations";

    private final AirQualityProviderPort provider;

    @Cacheable(STATIONS_CACHE)
    public StationIndex load() {
        List<Station> all = provider.findAllStations();

        return new StationIndex(
            all,
            all.stream().collect(Collectors.toUnmodifiableMap(Station::id, station -> station))
        );
    }

    @CacheEvict(value = STATIONS_CACHE, allEntries = true)
    @SuppressWarnings("java:S1186")
    public void refresh() {
    }

    public record StationIndex(List<Station> all, Map<Integer, Station> byId) {
        public StationIndex {
            all = List.copyOf(all);
            byId = Map.copyOf(byId);
        }
    }
}
